package com.smartlogix.pedidos.service.impl;

import com.smartlogix.pedidos.client.InventarioClient;
import com.smartlogix.pedidos.client.ProductoResponse;
import com.smartlogix.pedidos.event.PedidoCreadoEvent;
import com.smartlogix.pedidos.event.PedidoEventPublisher;
import com.smartlogix.pedidos.factory.PedidoFactory;
import com.smartlogix.pedidos.model.dto.PedidoDTO;
import com.smartlogix.pedidos.model.entity.Pedido;
import com.smartlogix.pedidos.repository.PedidoRepository;
import com.smartlogix.pedidos.service.PedidoService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final PedidoFactory pedidoFactory;
    private final InventarioClient inventarioClient;
    private final PedidoEventPublisher eventPublisher;

    @Override
    @Transactional
    @CircuitBreaker(name = "inventarioCB", fallbackMethod = "crearPedidoFallback")
    public PedidoDTO crearPedido(PedidoDTO pedidoDTO) {
        log.info("Creando pedido. SKU: {}, PayPal Order ID: {}",
                pedidoDTO.getSkuProducto(), pedidoDTO.getPaypalOrderId());

        // 1. Obtener producto del inventario (síncrono via Feign)
        ProductoResponse producto = inventarioClient.obtenerProducto(pedidoDTO.getSkuProducto());

        // 2. Validar stock
        if (producto.getStockActual() < pedidoDTO.getCantidad()) {
            throw new RuntimeException("Stock insuficiente en el inventario.");
        }

        // 3. Descontar stock (síncrono — garantiza consistencia antes de confirmar)
        inventarioClient.descontarStock(pedidoDTO.getSkuProducto(), pedidoDTO.getCantidad());

        // 4. Calcular precio total
        BigDecimal precioTotal = producto.getPrecio().multiply(BigDecimal.valueOf(pedidoDTO.getCantidad()));

        // 5. Guardar pedido con referencia PayPal
        Pedido entity = pedidoFactory.toEntity(pedidoDTO);
        entity.setPrecioTotal(precioTotal);
        entity.setEstado("COMPLETADO");
        entity.setPaypalOrderId(pedidoDTO.getPaypalOrderId());

        Pedido guardado = pedidoRepository.save(entity);

        // 6. Publicar evento asíncrono → RabbitMQ (Event-Driven)
        //    ms-inventario puede escucharlo para auditoría, notificaciones, etc.
        eventPublisher.publicarPedidoCreado(new PedidoCreadoEvent(
                guardado.getId(),
                guardado.getSkuProducto(),
                guardado.getCantidad(),
                guardado.getPaypalOrderId(),
                guardado.getEstado()
        ));

        PedidoDTO responseDTO = pedidoFactory.toDTO(guardado);
        responseDTO.setMensaje("Pedido procesado y pago PayPal confirmado.");
        return responseDTO;
    }

    public PedidoDTO crearPedidoFallback(PedidoDTO pedidoDTO, Throwable t) {
        log.error("Circuit Breaker activado. Fallo al comunicarse con ms-inventario: {}", t.getMessage());

        Pedido entity = pedidoFactory.toEntity(pedidoDTO);
        entity.setPrecioTotal(BigDecimal.ZERO);
        entity.setEstado("FALLIDO");
        entity.setPaypalOrderId(pedidoDTO.getPaypalOrderId());

        Pedido guardado = pedidoRepository.save(entity);
        PedidoDTO responseDTO = pedidoFactory.toDTO(guardado);
        responseDTO.setMensaje("El servicio de inventario no está disponible. Pedido registrado como FALLIDO.");
        return responseDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoDTO> listarPedidos() {
        return pedidoRepository.findAll()
                .stream()
                .map(pedidoFactory::toDTO)
                .collect(Collectors.toList());
    }
}

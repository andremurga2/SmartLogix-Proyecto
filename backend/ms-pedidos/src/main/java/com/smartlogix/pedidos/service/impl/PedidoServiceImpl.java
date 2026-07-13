package com.smartlogix.pedidos.service.impl;

import com.smartlogix.pedidos.client.InventarioClient;
import com.smartlogix.pedidos.client.ProductoResponse;
import com.smartlogix.pedidos.event.PedidoCreadoEvent;
import com.smartlogix.pedidos.event.PedidoEventPublisher;
import com.smartlogix.pedidos.factory.PedidoFactory;
import com.smartlogix.pedidos.model.dto.PedidoDTO;
import com.smartlogix.pedidos.model.dto.PedidoItemDTO;
import com.smartlogix.pedidos.model.entity.Pedido;
import com.smartlogix.pedidos.model.entity.PedidoItem;
import com.smartlogix.pedidos.repository.PedidoRepository;
import com.smartlogix.pedidos.service.PedidoService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
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
        if (pedidoDTO.getItems() == null || pedidoDTO.getItems().isEmpty()) {
            throw new IllegalArgumentException("El pedido debe contener al menos un producto.");
        }

        log.info("Creando pedido con {} ítem(s). PayPal Order ID: {}",
                pedidoDTO.getItems().size(), pedidoDTO.getPaypalOrderId());

        Pedido pedido = pedidoFactory.toEntity(pedidoDTO);
        BigDecimal totalPedido = BigDecimal.ZERO;

        // Registro de compensación tipo Saga: cada ítem cuyo stock ya fue
        // descontado en ms-inventario se guarda aquí. Si un ítem posterior
        // falla, o si falla el guardado del pedido en base de datos, se
        // revierte todo lo ya descontado antes de relanzar la excepción,
        // evitando que ms-inventario quede con stock descontado para un
        // pedido que nunca se persistió.
        List<PedidoItemDTO> itemsConfirmados = new ArrayList<>();
        Pedido guardado;

        try {
            for (PedidoItemDTO itemDTO : pedidoDTO.getItems()) {
                ProductoResponse producto = inventarioClient.obtenerProducto(itemDTO.getSkuProducto());

                if (producto.getStockActual() < itemDTO.getCantidad()) {
                    throw new RuntimeException(
                            "Stock insuficiente para " + itemDTO.getSkuProducto() +
                            " (disponible: " + producto.getStockActual() + ", solicitado: " + itemDTO.getCantidad() + ")");
                }

                inventarioClient.descontarStock(itemDTO.getSkuProducto(), itemDTO.getCantidad());
                itemsConfirmados.add(itemDTO);

                BigDecimal subtotal = producto.getPrecio().multiply(BigDecimal.valueOf(itemDTO.getCantidad()));
                totalPedido = totalPedido.add(subtotal);

                PedidoItem itemEntity = pedidoFactory.toItemEntity(itemDTO);
                itemEntity.setPrecioUnitario(producto.getPrecio());
                itemEntity.setSubtotal(subtotal);
                pedido.addItem(itemEntity);
            }

            // 2. Guardar pedido + items (cascade ALL los persiste juntos).
            // Se hace dentro del mismo try para que un fallo aquí (p. ej.
            // constraint de BD, conexión caída) también dispare la
            // compensación de stock: ya se descontó en ms-inventario pero
            // el pedido nunca quedará persistido.
            pedido.setPrecioTotal(totalPedido);
            pedido.setEstado("COMPLETADO");
            pedido.setPaypalOrderId(pedidoDTO.getPaypalOrderId());

            guardado = pedidoRepository.save(pedido);
        } catch (RuntimeException ex) {
            log.warn("Fallo creando pedido, revirtiendo stock de {} ítem(s) ya descontado(s): {}",
                    itemsConfirmados.size(), ex.getMessage());
            for (PedidoItemDTO confirmado : itemsConfirmados) {
                try {
                    inventarioClient.revertirStock(confirmado.getSkuProducto(), confirmado.getCantidad());
                } catch (Exception revertEx) {
                    // Si incluso la reversión falla (ms-inventario caído), lo dejamos
                    // registrado para reconciliación manual/job en vez de perder el evento.
                    log.error("No se pudo revertir stock de {} tras fallo de pedido: {}",
                            confirmado.getSkuProducto(), revertEx.getMessage());
                }
            }
            throw ex;
        }

        // 3. Publicar un evento por cada ítem (mantiene compatibilidad con el listener actual)
        for (PedidoItem item : guardado.getItems()) {
            eventPublisher.publicarPedidoCreado(new PedidoCreadoEvent(
                    guardado.getId(),
                    item.getSkuProducto(),
                    item.getCantidad(),
                    guardado.getPaypalOrderId(),
                    guardado.getEstado()
            ));
        }

        PedidoDTO responseDTO = pedidoFactory.toDTO(guardado);
        responseDTO.setMensaje("Pedido procesado y pago PayPal confirmado.");
        return responseDTO;
    }

    public PedidoDTO crearPedidoFallback(PedidoDTO pedidoDTO, Throwable t) {
        log.error("Circuit Breaker activado. Fallo al comunicarse con ms-inventario: {}", t.getMessage());

        Pedido pedido = pedidoFactory.toEntity(pedidoDTO);
        pedido.setPrecioTotal(BigDecimal.ZERO);
        pedido.setEstado("FALLIDO");
        pedido.setPaypalOrderId(pedidoDTO.getPaypalOrderId());

        if (pedidoDTO.getItems() != null) {
            for (PedidoItemDTO itemDTO : pedidoDTO.getItems()) {
                PedidoItem itemEntity = pedidoFactory.toItemEntity(itemDTO);
                pedido.addItem(itemEntity);
            }
        }

        Pedido guardado = pedidoRepository.save(pedido);
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
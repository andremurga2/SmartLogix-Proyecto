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

        // 1. Validar y descontar stock de CADA ítem (síncrono, dentro de la misma transacción).
        //    Si cualquier ítem falla, @Transactional hace rollback de todo el pedido,
        //    pero el stock ya descontado en ms-inventario para ítems previos en este
        //    loop NO se revierte automáticamente (ver nota más abajo).
        for (PedidoItemDTO itemDTO : pedidoDTO.getItems()) {
            ProductoResponse producto = inventarioClient.obtenerProducto(itemDTO.getSkuProducto());

            if (producto.getStockActual() < itemDTO.getCantidad()) {
                throw new RuntimeException(
                        "Stock insuficiente para " + itemDTO.getSkuProducto() +
                        " (disponible: " + producto.getStockActual() + ", solicitado: " + itemDTO.getCantidad() + ")");
            }

            inventarioClient.descontarStock(itemDTO.getSkuProducto(), itemDTO.getCantidad());

            BigDecimal subtotal = producto.getPrecio().multiply(BigDecimal.valueOf(itemDTO.getCantidad()));
            totalPedido = totalPedido.add(subtotal);

            PedidoItem itemEntity = pedidoFactory.toItemEntity(itemDTO);
            itemEntity.setPrecioUnitario(producto.getPrecio());
            itemEntity.setSubtotal(subtotal);
            pedido.addItem(itemEntity);
        }

        // 2. Guardar pedido + items (cascade ALL los persiste juntos)
        pedido.setPrecioTotal(totalPedido);
        pedido.setEstado("COMPLETADO");
        pedido.setPaypalOrderId(pedidoDTO.getPaypalOrderId());

        Pedido guardado = pedidoRepository.save(pedido);

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
package com.smartlogix.pedidos.factory;

import com.smartlogix.pedidos.model.dto.PedidoDTO;
import com.smartlogix.pedidos.model.dto.PedidoItemDTO;
import com.smartlogix.pedidos.model.entity.Pedido;
import com.smartlogix.pedidos.model.entity.PedidoItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PedidoFactory {

    /** Crea la cabecera de un pedido nuevo (sin items todavía — se agregan con addItem). */
    public Pedido toEntity(PedidoDTO dto) {
        return Pedido.builder()
                .precioTotal(dto.getPrecioTotal() != null ? dto.getPrecioTotal() : BigDecimal.ZERO)
                .estado("PENDIENTE")
                .paypalOrderId(dto.getPaypalOrderId())
                .build();
    }

    public PedidoItem toItemEntity(PedidoItemDTO dto) {
        return PedidoItem.builder()
                .skuProducto(dto.getSkuProducto())
                .cantidad(dto.getCantidad())
                .precioUnitario(dto.getPrecioUnitario() != null ? dto.getPrecioUnitario() : BigDecimal.ZERO)
                .subtotal(dto.getSubtotal() != null ? dto.getSubtotal() : BigDecimal.ZERO)
                .build();
    }

    public PedidoDTO toDTO(Pedido entity) {
        List<PedidoItemDTO> itemsDTO = entity.getItems().stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());

        return PedidoDTO.builder()
                .id(entity.getId())
                .items(itemsDTO)
                .precioTotal(entity.getPrecioTotal())
                .estado(entity.getEstado())
                .paypalOrderId(entity.getPaypalOrderId())
                .build();
    }

    public PedidoItemDTO toItemDTO(PedidoItem item) {
        return PedidoItemDTO.builder()
                .skuProducto(item.getSkuProducto())
                .cantidad(item.getCantidad())
                .precioUnitario(item.getPrecioUnitario())
                .subtotal(item.getSubtotal())
                .build();
    }
}
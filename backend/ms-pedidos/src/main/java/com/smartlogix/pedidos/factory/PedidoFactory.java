package com.smartlogix.pedidos.factory;

import com.smartlogix.pedidos.model.dto.PedidoDTO;
import com.smartlogix.pedidos.model.entity.Pedido;
import org.springframework.stereotype.Component;

@Component
public class PedidoFactory {

    public Pedido toEntity(PedidoDTO dto) {
        return Pedido.builder()
                .skuProducto(dto.getSkuProducto())
                .cantidad(dto.getCantidad())
                .precioTotal(dto.getPrecioTotal() != null ? dto.getPrecioTotal() : java.math.BigDecimal.ZERO)
                .estado("PENDIENTE")
                .paypalOrderId(dto.getPaypalOrderId())
                .build();
    }

    public PedidoDTO toDTO(Pedido entity) {
        return PedidoDTO.builder()
                .id(entity.getId())
                .skuProducto(entity.getSkuProducto())
                .cantidad(entity.getCantidad())
                .precioTotal(entity.getPrecioTotal())
                .estado(entity.getEstado())
                .paypalOrderId(entity.getPaypalOrderId())
                .build();
    }
}

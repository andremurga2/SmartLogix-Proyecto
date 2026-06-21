package com.smartlogix.pedidos.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDTO {
    private Long id;

    @Builder.Default
    private List<PedidoItemDTO> items = new ArrayList<>();

    private BigDecimal precioTotal;
    private String estado;
    private String mensaje;
    private String paypalOrderId;
}
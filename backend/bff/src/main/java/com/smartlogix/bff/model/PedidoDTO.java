package com.smartlogix.bff.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDTO {
    private String skuProducto;
    private Integer cantidad;
    private BigDecimal precioTotal;
    private String estado;
    private String mensaje;
    private String paypalOrderId;   // ← nuevo campo
}

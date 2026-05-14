package com.smartlogix.inventario.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoCreadoEvent implements Serializable {
    private Long   pedidoId;
    private String skuProducto;
    private Integer cantidad;
    private String paypalOrderId;
    private String estado;
}

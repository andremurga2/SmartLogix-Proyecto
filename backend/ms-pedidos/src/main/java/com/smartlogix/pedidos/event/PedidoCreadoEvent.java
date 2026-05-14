package com.smartlogix.pedidos.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Evento publicado en RabbitMQ cuando un pedido queda COMPLETADO.
 * ms-inventario lo consume para confirmar el descuento de stock
 * de forma asíncrona (Event-Driven Architecture).
 */
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

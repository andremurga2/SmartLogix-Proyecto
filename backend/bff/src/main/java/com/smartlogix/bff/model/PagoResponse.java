package com.smartlogix.bff.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagoResponse {
    private boolean exitoso;
    private String mensaje;
    private String paypalOrderId;
    private String estadoPago;
    private PedidoDTO pedido;
}

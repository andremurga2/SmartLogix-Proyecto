package com.smartlogix.bff.model;

import lombok.Data;

@Data
public class CrearOrdenRequest {
    private String skuProducto;
    private Integer cantidad;
    private String monto;       // ej: "29.99"
    private String moneda;      // ej: "USD"
    private String descripcion;
}

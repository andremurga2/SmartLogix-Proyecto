package com.smartlogix.pedidos.client;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductoResponse {
    private String sku;
    private String nombre;
    private BigDecimal precio;
    private Integer stockActual;
    private boolean disponible;
}

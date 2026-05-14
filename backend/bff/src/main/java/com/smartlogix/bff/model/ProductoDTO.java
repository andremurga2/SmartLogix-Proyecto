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
public class ProductoDTO {
    private String sku;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer stockActual;
    private boolean disponible;
}

package com.smartlogix.bff.model;

import lombok.Data;

import java.util.List;

@Data
public class CrearOrdenRequest {
    private List<ItemCarritoDTO> items;
    private String moneda; // ej: "USD"
}
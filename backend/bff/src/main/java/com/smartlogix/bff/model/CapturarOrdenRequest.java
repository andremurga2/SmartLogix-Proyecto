package com.smartlogix.bff.model;

import lombok.Data;

@Data
public class CapturarOrdenRequest {
    private String orderId;
    private String skuProducto;
    private Integer cantidad;
}

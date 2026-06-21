package com.smartlogix.bff.model;

import lombok.Data;

import java.util.List;

@Data
public class CapturarOrdenRequest {
    private String orderId;
    private List<ItemCarritoDTO> items;
}
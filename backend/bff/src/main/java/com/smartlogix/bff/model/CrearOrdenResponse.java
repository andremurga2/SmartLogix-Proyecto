package com.smartlogix.bff.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CrearOrdenResponse {
    private String orderId;
    private String approveUrl;
    private String status;
}

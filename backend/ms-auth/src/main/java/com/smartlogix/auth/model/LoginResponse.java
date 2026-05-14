package com.smartlogix.auth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private boolean success;
    private String message;
    private String token;      // JWT — null si login fallido
    private String username;
    private String role;

    /** Constructor para respuesta de error */
    public LoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}

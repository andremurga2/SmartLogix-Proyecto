package com.smartlogix.auth.service;

import com.smartlogix.auth.model.LoginResponse;
import com.smartlogix.auth.model.ValidateResponse;
import com.smartlogix.auth.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtUtil jwtUtil;

    /** Usuarios en memoria (mismos que tenía el BFF original) */
    private static final Map<String, String[]> USERS = new HashMap<>();

    static {
        USERS.put("admin",  new String[]{"admin123",   "ADMIN"});
        USERS.put("user1",  new String[]{"password123", "USER"});
        USERS.put("user2",  new String[]{"password456", "USER"});
    }

    public LoginResponse login(String username, String password) {
        String[] userData = USERS.get(username);

        if (userData == null || !userData[0].equals(password)) {
            log.warn("Intento de login fallido para usuario: {}", username);
            return new LoginResponse(false, "Usuario o contraseña inválidos");
        }

        String role  = userData[1];
        String token = jwtUtil.generateToken(username, role);
        log.info("Login exitoso para: {} [{}]", username, role);

        return new LoginResponse(true, "Login exitoso", token, username, role);
    }

    public ValidateResponse validate(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            return new ValidateResponse(false, null, null, "Token ausente o mal formado");
        }

        String token = bearerToken.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            return new ValidateResponse(false, null, null, "Token inválido o expirado");
        }

        Claims claims = jwtUtil.validateToken(token);
        return new ValidateResponse(true, claims.getSubject(),
                claims.get("role", String.class), "Token válido");
    }
}

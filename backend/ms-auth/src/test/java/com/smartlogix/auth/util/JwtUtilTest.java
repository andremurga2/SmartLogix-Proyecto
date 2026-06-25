package com.smartlogix.auth.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Inyectamos los valores que normalmente vienen de application.yml
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "smartlogix-secret-key-must-be-at-least-32-chars!!");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600000L);
    }

    @Test
    void debeGenerarTokenNoNulo() {
        String token = jwtUtil.generateToken("admin", "ADMIN");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void debeValidarTokenGenerado() {
        String token = jwtUtil.generateToken("admin", "ADMIN");
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void debeExtraerUsernameDelToken() {
        String token = jwtUtil.generateToken("admin", "ADMIN");
        Claims claims = jwtUtil.validateToken(token);
        assertEquals("admin", claims.getSubject());
    }

    @Test
    void debeExtraerRolDelToken() {
        String token = jwtUtil.generateToken("admin", "ADMIN");
        Claims claims = jwtUtil.validateToken(token);
        assertEquals("ADMIN", claims.get("role", String.class));
    }

    @Test
    void debeRetornarFalsoParaTokenInvalido() {
        assertFalse(jwtUtil.isTokenValid("token.invalido.abc"));
    }

    @Test
    void debeRetornarFalsoParaTokenVacio() {
        assertFalse(jwtUtil.isTokenValid(""));
    }
}
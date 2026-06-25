package com.smartlogix.auth.controller;

import com.smartlogix.auth.model.LoginRequest;
import com.smartlogix.auth.model.LoginResponse;
import com.smartlogix.auth.model.UsuarioDTO;
import com.smartlogix.auth.model.ValidateResponse;
import com.smartlogix.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private UsuarioDTO usuarioDTO;

    @BeforeEach
    void setUp() {
        usuarioDTO = new UsuarioDTO();
        usuarioDTO.setUsername("admin");
        usuarioDTO.setRole("ADMIN");
        usuarioDTO.setActivo(true);
    }

    // ── LOGIN ──────────────────────────────────────────────────────────────────

    @Test
    void loginDebeRetornar200CuandoEsExitoso() {
        LoginResponse loginResponse = new LoginResponse(true, "Login exitoso", "jwt-token", "admin", "ADMIN");
        when(authService.login("admin", "pass")).thenReturn(loginResponse);

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("pass");

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("jwt-token", response.getBody().getToken());
    }

    @Test
    void loginDebeRetornar401CuandoFalla() {
        LoginResponse loginResponse = new LoginResponse(false, "Credenciales inválidas");
        when(authService.login("x", "x")).thenReturn(loginResponse);

        LoginRequest request = new LoginRequest();
        request.setUsername("x");
        request.setPassword("x");

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
    }

    // ── VALIDATE ───────────────────────────────────────────────────────────────

    @Test
    void validateDebeRetornar200CuandoTokenEsValido() {
        ValidateResponse validateResponse = new ValidateResponse(true, "admin", "ADMIN", "Token válido");
        when(authService.validate("Bearer jwt-token")).thenReturn(validateResponse);

        ResponseEntity<ValidateResponse> response = authController.validate("Bearer jwt-token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isValid());
    }

    @Test
    void validateDebeRetornar401CuandoTokenEsInvalido() {
        ValidateResponse validateResponse = new ValidateResponse(false, null, null, "Token inválido");
        when(authService.validate(null)).thenReturn(validateResponse);

        ResponseEntity<ValidateResponse> response = authController.validate(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertFalse(response.getBody().isValid());
    }

    // ── USUARIOS ───────────────────────────────────────────────────────────────

    @Test
    void listarUsuariosDebeRetornar200() {
        when(authService.listarUsuarios()).thenReturn(List.of(usuarioDTO));

        ResponseEntity<List<UsuarioDTO>> response = authController.listarUsuarios();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void crearUsuarioDebeRetornar200CuandoEsExitoso() {
        when(authService.crearUsuario(usuarioDTO)).thenReturn(usuarioDTO);

        ResponseEntity<?> response = authController.crearUsuario(usuarioDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void crearUsuarioDebeRetornar400CuandoYaExiste() {
        when(authService.crearUsuario(usuarioDTO))
                .thenThrow(new IllegalArgumentException("El usuario ya existe"));

        ResponseEntity<?> response = authController.crearUsuario(usuarioDTO);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void registrarUsuarioDebeRetornar200() {
        when(authService.registrarUsuario(usuarioDTO)).thenReturn(usuarioDTO);

        ResponseEntity<?> response = authController.registrarUsuario(usuarioDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void actualizarUsuarioDebeRetornar200() {
        when(authService.actualizarUsuario(1L, usuarioDTO)).thenReturn(usuarioDTO);

        ResponseEntity<?> response = authController.actualizarUsuario(1L, usuarioDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void eliminarUsuarioDebeRetornar204() {
        doNothing().when(authService).eliminarUsuario(1L);

        ResponseEntity<?> response = authController.eliminarUsuario(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void eliminarUsuarioDebeRetornar400SiNoExiste() {
        doThrow(new IllegalArgumentException("Usuario no encontrado"))
                .when(authService).eliminarUsuario(99L);

        ResponseEntity<?> response = authController.eliminarUsuario(99L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
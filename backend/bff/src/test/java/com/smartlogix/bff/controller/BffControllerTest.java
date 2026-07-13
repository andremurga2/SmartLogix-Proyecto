package com.smartlogix.bff.controller;

import com.smartlogix.bff.client.AuthClient;
import com.smartlogix.bff.model.*;
import com.smartlogix.bff.service.BffService;
import feign.FeignException;
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
class BffControllerTest {

    @Mock private BffService bffService;
    @Mock private AuthClient authClient;

    @InjectMocks
    private BffController bffController;

    private ValidateResponse validAdmin;
    private ValidateResponse invalidToken;
    private ValidateResponse validUser;

    @BeforeEach
    void setUp() {
        validAdmin = new ValidateResponse(true, "admin", "ADMIN", "Token válido");
        validUser  = new ValidateResponse(true, "user",  "USER",  "Token válido");
        invalidToken = new ValidateResponse(false, null, null, "Token inválido");
    }

    // ── CATÁLOGO ──────────────────────────────────────────────────────────────

    @Test
    void obtenerCatalogoDebeRetornar200() {
        ProductoDTO p = new ProductoDTO();
        p.setSku("SKU-001");
        when(bffService.obtenerCatalogo()).thenReturn(List.of(p));

        ResponseEntity<List<ProductoDTO>> response = bffController.obtenerCatalogo();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    // ── COMPRAR ───────────────────────────────────────────────────────────────

    @Test
    void realizarCompraDebeRetornar200ConTokenValido() {
        when(authClient.validate("Bearer token")).thenReturn(validAdmin);

        PedidoDTO pedido = new PedidoDTO();
        pedido.setEstado("COMPLETADO");
        when(bffService.realizarCompra(pedido)).thenReturn(pedido);

        ResponseEntity<?> response = bffController.realizarCompra("Bearer token", pedido);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void realizarCompraDebeRetornar401SinToken() {
        ResponseEntity<?> response = bffController.realizarCompra(null, new PedidoDTO());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(bffService);
    }

    @Test
    void realizarCompraDebeRetornar401ConTokenInvalido() {
        when(authClient.validate("Bearer bad")).thenReturn(invalidToken);

        ResponseEntity<?> response = bffController.realizarCompra("Bearer bad", new PedidoDTO());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void realizarCompraDebeRetornar401SiValidateThrows() {
        when(authClient.validate("Bearer err")).thenThrow(new RuntimeException("timeout"));

        ResponseEntity<?> response = bffController.realizarCompra("Bearer err", new PedidoDTO());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // ── VALIDATE ─────────────────────────────────────────────────────────────

    @Test
    void validateDebeRetornar200ConTokenValido() {
        when(authClient.validate("Bearer ok")).thenReturn(validAdmin);

        ResponseEntity<ValidateResponse> response = bffController.validate("Bearer ok");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isValid());
    }

    @Test
    void validateDebeRetornar401ConTokenInvalido() {
        when(authClient.validate("Bearer bad")).thenReturn(invalidToken);

        ResponseEntity<ValidateResponse> response = bffController.validate("Bearer bad");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void validateDebeRetornar401SiFeignLanzaUnauthorized() {
        FeignException.Unauthorized ex = mock(FeignException.Unauthorized.class);
        when(authClient.validate("Bearer bad")).thenThrow(ex);

        ResponseEntity<ValidateResponse> response = bffController.validate("Bearer bad");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // ── REGISTRO ─────────────────────────────────────────────────────────────

    @Test
    void registrarUsuarioDebeRetornar200() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setUsername("nuevo");
        when(bffService.registrarUsuario(dto)).thenReturn(dto);

        ResponseEntity<?> response = bffController.registrarUsuario(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void registrarUsuarioDebeRetornar503SiServicioNoDisponible() {
        UsuarioDTO dto = new UsuarioDTO();
        when(bffService.registrarUsuario(dto)).thenThrow(new RuntimeException("connection error"));

        ResponseEntity<?> response = bffController.registrarUsuario(dto);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    // ── ADMIN: PRODUCTOS ──────────────────────────────────────────────────────

    @Test
    void crearProductoDebeRetornar200ConAdminToken() {
        when(authClient.validate("Bearer admin")).thenReturn(validAdmin);
        ProductoDTO p = new ProductoDTO();
        p.setSku("SKU-NEW");
        when(bffService.crearProducto(p)).thenReturn(p);

        ResponseEntity<?> response = bffController.crearProducto("Bearer admin", p);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void crearProductoDebeRetornar403ConTokenDeUser() {
        when(authClient.validate("Bearer user")).thenReturn(validUser);

        ResponseEntity<?> response = bffController.crearProducto("Bearer user", new ProductoDTO());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(bffService);
    }

    @Test
    void crearProductoDebeRetornar403SinToken() {
        ResponseEntity<?> response = bffController.crearProducto(null, new ProductoDTO());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void actualizarProductoDebeRetornar200ConAdminToken() {
        when(authClient.validate("Bearer admin")).thenReturn(validAdmin);
        ProductoDTO p = new ProductoDTO();
        when(bffService.actualizarProducto("SKU-001", p)).thenReturn(p);

        ResponseEntity<?> response = bffController.actualizarProducto("Bearer admin", "SKU-001", p);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void actualizarProductoDebeRetornar403SinAdmin() {
        when(authClient.validate("Bearer user")).thenReturn(validUser);

        ResponseEntity<?> response = bffController.actualizarProducto("Bearer user", "SKU-001", new ProductoDTO());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void eliminarProductoDebeRetornar204ConAdminToken() {
        when(authClient.validate("Bearer admin")).thenReturn(validAdmin);
        doNothing().when(bffService).eliminarProducto("SKU-001");

        ResponseEntity<?> response = bffController.eliminarProducto("Bearer admin", "SKU-001");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void eliminarProductoDebeRetornar403SinAdmin() {
        when(authClient.validate("Bearer user")).thenReturn(validUser);

        ResponseEntity<?> response = bffController.eliminarProducto("Bearer user", "SKU-001");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // ── ADMIN: PEDIDOS ────────────────────────────────────────────────────────

    @Test
    void listarPedidosDebeRetornar200ConAdminToken() {
        when(authClient.validate("Bearer admin")).thenReturn(validAdmin);
        when(bffService.listarPedidos()).thenReturn(List.of(new PedidoDTO()));

        ResponseEntity<?> response = bffController.listarPedidos("Bearer admin");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void listarPedidosDebeRetornar403SinAdmin() {
        when(authClient.validate("Bearer user")).thenReturn(validUser);

        ResponseEntity<?> response = bffController.listarPedidos("Bearer user");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // ── ADMIN: USUARIOS ───────────────────────────────────────────────────────
    // Estos mocks ahora incluyen "Bearer admin" como primer argumento porque
    // BffService reenvía el header Authorization hacia ms-auth (ver punto 3).

    @Test
    void listarUsuariosDebeRetornar200ConAdminToken() {
        when(authClient.validate("Bearer admin")).thenReturn(validAdmin);
        when(bffService.listarUsuarios("Bearer admin")).thenReturn(List.of(new UsuarioDTO()));

        ResponseEntity<?> response = bffController.listarUsuarios("Bearer admin");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void listarUsuariosDebeRetornar403SinAdmin() {
        when(authClient.validate("Bearer user")).thenReturn(validUser);

        ResponseEntity<?> response = bffController.listarUsuarios("Bearer user");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(bffService);
    }

    @Test
    void crearUsuarioAdminDebeRetornar200() {
        when(authClient.validate("Bearer admin")).thenReturn(validAdmin);
        UsuarioDTO dto = new UsuarioDTO();
        when(bffService.crearUsuario("Bearer admin", dto)).thenReturn(dto);

        ResponseEntity<?> response = bffController.crearUsuario("Bearer admin", dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void actualizarUsuarioAdminDebeRetornar200() {
        when(authClient.validate("Bearer admin")).thenReturn(validAdmin);
        UsuarioDTO dto = new UsuarioDTO();
        when(bffService.actualizarUsuario("Bearer admin", 1L, dto)).thenReturn(dto);

        ResponseEntity<?> response = bffController.actualizarUsuario("Bearer admin", 1L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void eliminarUsuarioAdminDebeRetornar204() {
        when(authClient.validate("Bearer admin")).thenReturn(validAdmin);
        doNothing().when(bffService).eliminarUsuario("Bearer admin", 1L);

        ResponseEntity<?> response = bffController.eliminarUsuario("Bearer admin", 1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void eliminarUsuarioDebeRetornar403SinAdmin() {
        when(authClient.validate("Bearer user")).thenReturn(validUser);

        ResponseEntity<?> response = bffController.eliminarUsuario("Bearer user", 1L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
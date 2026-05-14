package com.smartlogix.bff.controller;

import com.smartlogix.bff.client.AuthClient;
import com.smartlogix.bff.model.*;
import com.smartlogix.bff.service.BffService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class BffController {

    private final BffService bffService;
    private final AuthClient authClient;

    // ── Catálogo público ───────────────────────────────────────────────────────
    @GetMapping("/store/catalogo")
    public ResponseEntity<List<ProductoDTO>> obtenerCatalogo() {
        return ResponseEntity.ok(bffService.obtenerCatalogo());
    }

    // ── Compra (requiere JWT) ──────────────────────────────────────────────────
    @PostMapping("/store/comprar")
    public ResponseEntity<?> realizarCompra(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody PedidoDTO pedidoDTO) {
        if (!isTokenValid(authHeader)) {
            return ResponseEntity.status(401).body("Token inválido o ausente.");
        }
        return ResponseEntity.ok(bffService.realizarCompra(pedidoDTO));
    }

    // ── Auth ───────────────────────────────────────────────────────────────────
    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        try {
            org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
            String authUrl = System.getenv().getOrDefault("AUTH_SERVICE_URL", "http://localhost:8083");
            LoginResponse response = rt.postForObject(
                    authUrl + "/api/auth/login", loginRequest, LoginResponse.class);
            if (response != null && response.isSuccess()) {
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(401).body(
                    new LoginResponse(false, "Usuario o contraseña inválidos", null, null, null));
        } catch (Exception e) {
            log.error("Error llamando a ms-auth: {}", e.getMessage());
            return ResponseEntity.status(503).body(
                    new LoginResponse(false, "Servicio de autenticación no disponible", null, null, null));
        }
    }

    @GetMapping("/auth/validate")
    public ResponseEntity<ValidateResponse> validate(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            ValidateResponse response = authClient.validate(authHeader);
            return response.isValid() ? ResponseEntity.ok(response)
                    : ResponseEntity.status(401).body(response);
        } catch (FeignException.Unauthorized e) {
            return ResponseEntity.status(401).body(
                    new ValidateResponse(false, null, null, "Token inválido"));
        }
    }

    // ── Admin: Productos ───────────────────────────────────────────────────────
    @PostMapping("/admin/productos")
    public ResponseEntity<?> crearProducto(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody ProductoDTO productoDTO) {
        if (!isAdminToken(authHeader)) return ResponseEntity.status(403).body("Acceso denegado.");
        return ResponseEntity.ok(bffService.crearProducto(productoDTO));
    }

    @PutMapping("/admin/productos/{sku}")
    public ResponseEntity<?> actualizarProducto(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String sku,
            @RequestBody ProductoDTO productoDTO) {
        if (!isAdminToken(authHeader)) return ResponseEntity.status(403).body("Acceso denegado.");
        return ResponseEntity.ok(bffService.actualizarProducto(sku, productoDTO));
    }

    @DeleteMapping("/admin/productos/{sku}")
    public ResponseEntity<?> eliminarProducto(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String sku) {
        if (!isAdminToken(authHeader)) return ResponseEntity.status(403).body("Acceso denegado.");
        bffService.eliminarProducto(sku);
        return ResponseEntity.noContent().build();
    }

    // ── Admin: Pedidos ─────────────────────────────────────────────────────────
    @GetMapping("/admin/pedidos")
    public ResponseEntity<?> listarPedidos(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdminToken(authHeader)) return ResponseEntity.status(403).body("Acceso denegado.");
        return ResponseEntity.ok(bffService.listarPedidos());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private boolean isTokenValid(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) return false;
        try {
            ValidateResponse r = authClient.validate(authHeader);
            return r.isValid();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAdminToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) return false;
        try {
            ValidateResponse r = authClient.validate(authHeader);
            return r.isValid() && "ADMIN".equals(r.getRole());
        } catch (Exception e) {
            return false;
        }
    }
}

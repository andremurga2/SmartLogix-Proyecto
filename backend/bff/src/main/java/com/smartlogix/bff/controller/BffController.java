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
            LoginResponse response = authClient.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (FeignException.Unauthorized e) {
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

    /** POST /api/auth/registro — registro público (sin token), siempre rol USER */
    @PostMapping("/auth/registro")
    public ResponseEntity<?> registrarUsuario(@RequestBody UsuarioDTO usuarioDTO) {
        try {
            return ResponseEntity.ok(bffService.registrarUsuario(usuarioDTO));
        } catch (feign.FeignException.BadRequest e) {
            return ResponseEntity.badRequest().body(e.contentUTF8());
        } catch (Exception e) {
            log.error("Error en registro: {}", e.getMessage());
            return ResponseEntity.status(503).body("Servicio de autenticación no disponible");
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

    // ── Admin: Usuarios ────────────────────────────────────────────────────────
    // Estos 4 endpoints ahora reenvían el JWT hacia ms-auth (authHeader),
    // que a partir del punto 3 valida el token él mismo en vez de confiar
    // ciegamente en que el BFF ya filtró.
    @GetMapping("/admin/usuarios")
    public ResponseEntity<?> listarUsuarios(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdminToken(authHeader)) return ResponseEntity.status(403).body("Acceso denegado.");
        return ResponseEntity.ok(bffService.listarUsuarios(authHeader));
    }

    @PostMapping("/admin/usuarios")
    public ResponseEntity<?> crearUsuario(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UsuarioDTO usuarioDTO) {
        if (!isAdminToken(authHeader)) return ResponseEntity.status(403).body("Acceso denegado.");
        return ResponseEntity.ok(bffService.crearUsuario(authHeader, usuarioDTO));
    }

    @PutMapping("/admin/usuarios/{id}")
    public ResponseEntity<?> actualizarUsuario(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody UsuarioDTO usuarioDTO) {
        if (!isAdminToken(authHeader)) return ResponseEntity.status(403).body("Acceso denegado.");
        return ResponseEntity.ok(bffService.actualizarUsuario(authHeader, id, usuarioDTO));
    }

    @DeleteMapping("/admin/usuarios/{id}")
    public ResponseEntity<?> eliminarUsuario(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        if (!isAdminToken(authHeader)) return ResponseEntity.status(403).body("Acceso denegado.");
        bffService.eliminarUsuario(authHeader, id);
        return ResponseEntity.noContent().build();
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
package com.smartlogix.bff.controller;
import com.smartlogix.bff.client.AuthClient;
import com.smartlogix.bff.model.*;
import com.smartlogix.bff.service.BffService;
import feign.FeignException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "BFF", description = "Punto de entrada único para el frontend: agrega y orquesta llamadas a ms-auth, ms-inventario y ms-pedidos")
public class BffController {

    private final BffService bffService;
    private final AuthClient authClient;

    // ── Catálogo público ───────────────────────────────────────────────────────
    @Operation(summary = "Obtener catálogo", description = "Lista pública de productos disponibles, sin requerir autenticación.")
    @ApiResponse(responseCode = "200", description = "Catálogo de productos")
    @GetMapping("/store/catalogo")
    public ResponseEntity<List<ProductoDTO>> obtenerCatalogo() {
        return ResponseEntity.ok(bffService.obtenerCatalogo());
    }

    // ── Compra (requiere JWT) ──────────────────────────────────────────────────
    @Operation(
            summary = "Realizar compra",
            description = "Crea un pedido a partir del carrito. Requiere JWT válido en el header Authorization. " +
                    "Reenvía la creación a ms-pedidos, que descuenta stock contra ms-inventario y aplica " +
                    "compensación automática si algún ítem falla."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido creado"),
            @ApiResponse(responseCode = "401", description = "Token inválido o ausente")
    })
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
    @Operation(summary = "Login", description = "Reenvía las credenciales a ms-auth y devuelve el JWT si son válidas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas"),
            @ApiResponse(responseCode = "503", description = "ms-auth no disponible")
    })
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

    @Operation(summary = "Validar JWT", description = "Reenvía la validación del token a ms-auth.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token válido"),
            @ApiResponse(responseCode = "401", description = "Token inválido, ausente o expirado")
    })
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

    @Operation(
            summary = "Registro público",
            description = "Registro público sin token, siempre crea el usuario con rol USER."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario registrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (ej. username ya existe)"),
            @ApiResponse(responseCode = "503", description = "ms-auth no disponible")
    })
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
    @Operation(summary = "Crear producto (admin)", description = "Requiere JWT con rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto creado"),
            @ApiResponse(responseCode = "403", description = "Token ausente o sin rol ADMIN")
    })
    @PostMapping("/admin/productos")
    public ResponseEntity<?> crearProducto(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody ProductoDTO productoDTO) {
        if (!isAdminToken(authHeader)) return ResponseEntity.status(403).body("Acceso denegado.");
        return ResponseEntity.ok(bffService.crearProducto(productoDTO));
    }

    @Operation(summary = "Actualizar producto (admin)", description = "Requiere JWT con rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto actualizado"),
            @ApiResponse(responseCode = "403", description = "Token ausente o sin rol ADMIN")
    })
    @PutMapping("/admin/productos/{sku}")
    public ResponseEntity<?> actualizarProducto(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String sku,
            @RequestBody ProductoDTO productoDTO) {
        if (!isAdminToken(authHeader)) return ResponseEntity.status(403).body("Acceso denegado.");
        return ResponseEntity.ok(bffService.actualizarProducto(sku, productoDTO));
    }

    @Operation(summary = "Eliminar producto (admin)", description = "Requiere JWT con rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Producto eliminado"),
            @ApiResponse(responseCode = "403", description = "Token ausente o sin rol ADMIN")
    })
    @DeleteMapping("/admin/productos/{sku}")
    public ResponseEntity<?> eliminarProducto(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String sku) {
        if (!isAdminToken(authHeader)) return ResponseEntity.status(403).body("Acceso denegado.");
        bffService.eliminarProducto(sku);
        return ResponseEntity.noContent().build();
    }

    // ── Admin: Pedidos ─────────────────────────────────────────────────────────
    @Operation(summary = "Listar pedidos (admin)", description = "Requiere JWT con rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de pedidos"),
            @ApiResponse(responseCode = "403", description = "Token ausente o sin rol ADMIN")
    })
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
    @Operation(summary = "Listar usuarios (admin)", description = "Requiere JWT con rol ADMIN. El token se reenvía a ms-auth, que lo vuelve a validar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de usuarios"),
            @ApiResponse(responseCode = "403", description = "Token ausente o sin rol ADMIN")
    })
    @GetMapping("/admin/usuarios")
    public ResponseEntity<?> listarUsuarios(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdminToken(authHeader)) return ResponseEntity.status(403).body("Acceso denegado.");
        return ResponseEntity.ok(bffService.listarUsuarios(authHeader));
    }

    @Operation(summary = "Crear usuario (admin)", description = "Requiere JWT con rol ADMIN. El token se reenvía a ms-auth.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario creado"),
            @ApiResponse(responseCode = "403", description = "Token ausente o sin rol ADMIN")
    })
    @PostMapping("/admin/usuarios")
    public ResponseEntity<?> crearUsuario(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UsuarioDTO usuarioDTO) {
        if (!isAdminToken(authHeader)) return ResponseEntity.status(403).body("Acceso denegado.");
        return ResponseEntity.ok(bffService.crearUsuario(authHeader, usuarioDTO));
    }

    @Operation(summary = "Actualizar usuario (admin)", description = "Requiere JWT con rol ADMIN. El token se reenvía a ms-auth.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actualizado"),
            @ApiResponse(responseCode = "403", description = "Token ausente o sin rol ADMIN")
    })
    @PutMapping("/admin/usuarios/{id}")
    public ResponseEntity<?> actualizarUsuario(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody UsuarioDTO usuarioDTO) {
        if (!isAdminToken(authHeader)) return ResponseEntity.status(403).body("Acceso denegado.");
        return ResponseEntity.ok(bffService.actualizarUsuario(authHeader, id, usuarioDTO));
    }

    @Operation(summary = "Eliminar usuario (admin)", description = "Requiere JWT con rol ADMIN. El token se reenvía a ms-auth.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario eliminado"),
            @ApiResponse(responseCode = "403", description = "Token ausente o sin rol ADMIN")
    })
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
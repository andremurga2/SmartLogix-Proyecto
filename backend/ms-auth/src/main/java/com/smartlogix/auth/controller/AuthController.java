package com.smartlogix.auth.controller;

import com.smartlogix.auth.model.LoginRequest;
import com.smartlogix.auth.model.LoginResponse;
import com.smartlogix.auth.model.ValidateResponse;
import com.smartlogix.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smartlogix.auth.model.UsuarioDTO;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/login
     * Recibe credenciales y devuelve un JWT si son válidas.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request.getUsername(), request.getPassword());
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(401).body(response);
    }

    /**
     * GET /api/auth/validate
     * Verifica el JWT enviado en el header Authorization: Bearer <token>.
     * El BFF llama a este endpoint antes de enrutar peticiones protegidas.
     */
    @GetMapping("/validate")
    public ResponseEntity<ValidateResponse> validate(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        ValidateResponse response = authService.validate(authHeader);
        return response.isValid()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(401).body(response);
    }
    /** GET /api/auth/usuarios — lista todos los usuarios (sin password) */
    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {
        return ResponseEntity.ok(authService.listarUsuarios());
    }

    /** POST /api/auth/usuarios — crea un usuario nuevo */
    @PostMapping("/usuarios")
    public ResponseEntity<?> crearUsuario(@RequestBody UsuarioDTO dto) {
        try {
            return ResponseEntity.ok(authService.crearUsuario(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** PUT /api/auth/usuarios/{id} — actualiza rol/estado/password */
    @PutMapping("/usuarios/{id}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long id, @RequestBody UsuarioDTO dto) {
        try {
            return ResponseEntity.ok(authService.actualizarUsuario(id, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** DELETE /api/auth/usuarios/{id} — elimina un usuario */
    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long id) {
        try {
            authService.eliminarUsuario(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

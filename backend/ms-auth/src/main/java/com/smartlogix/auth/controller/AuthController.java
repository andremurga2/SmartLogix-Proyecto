package com.smartlogix.auth.controller;

import com.smartlogix.auth.model.LoginRequest;
import com.smartlogix.auth.model.LoginResponse;
import com.smartlogix.auth.model.ValidateResponse;
import com.smartlogix.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smartlogix.auth.model.UsuarioDTO;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Login, validación de JWT y gestión de usuarios")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Iniciar sesión",
            description = "Valida las credenciales del usuario y, si son correctas, devuelve un JWT " +
                    "firmado que debe enviarse en el header Authorization de las siguientes peticiones."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso, token generado"),
            @ApiResponse(responseCode = "401", description = "Usuario o contraseña incorrectos")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request.getUsername(), request.getPassword());
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(401).body(response);
    }

    @Operation(
            summary = "Validar JWT",
            description = "Verifica que el token recibido en el header Authorization (formato 'Bearer <token>') " +
                    "sea válido y no haya expirado. El BFF llama a este endpoint antes de enrutar peticiones protegidas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token válido"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado")
    })
    @GetMapping("/validate")
    public ResponseEntity<ValidateResponse> validate(
            @Parameter(description = "Header Authorization con formato 'Bearer <token>'")
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        ValidateResponse response = authService.validate(authHeader);
        return response.isValid()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(401).body(response);
    }

    @Operation(
            summary = "Listar usuarios",
            description = "Devuelve todos los usuarios registrados, sin exponer el campo de contraseña. Uso administrativo."
    )
    @ApiResponse(responseCode = "200", description = "Listado de usuarios")
    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {
        return ResponseEntity.ok(authService.listarUsuarios());
    }

    @Operation(
            summary = "Crear usuario (admin)",
            description = "Crea un usuario nuevo con el rol indicado en el DTO. A diferencia de /registro, " +
                    "permite definir el rol directamente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (ej. username ya existe)")
    })
    @PostMapping("/usuarios")
    public ResponseEntity<?> crearUsuario(@RequestBody UsuarioDTO dto) {
        try {
            return ResponseEntity.ok(authService.crearUsuario(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Registro público",
            description = "Permite a cualquier visitante crear una cuenta propia. El rol siempre se fuerza a " +
                    "USER, sin importar lo que venga en el body, para evitar auto-asignación de privilegios."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario registrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (ej. username ya existe)")
    })
    @PostMapping("/registro")
    public ResponseEntity<?> registrarUsuario(@RequestBody UsuarioDTO dto) {
        try {
            return ResponseEntity.ok(authService.registrarUsuario(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Actualizar usuario",
            description = "Actualiza rol, estado o contraseña de un usuario existente, identificado por id."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o usuario inexistente")
    })
    @PutMapping("/usuarios/{id}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long id, @RequestBody UsuarioDTO dto) {
        try {
            return ResponseEntity.ok(authService.actualizarUsuario(id, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Eliminar usuario",
            description = "Elimina definitivamente un usuario por id."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario eliminado"),
            @ApiResponse(responseCode = "400", description = "Usuario inexistente")
    })
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
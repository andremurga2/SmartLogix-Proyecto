package com.smartlogix.auth.service;
import com.smartlogix.auth.model.UsuarioDTO;
import java.util.List;
import com.smartlogix.auth.model.LoginResponse;
import com.smartlogix.auth.model.ValidateResponse;
import com.smartlogix.auth.model.entity.Usuario;
import com.smartlogix.auth.repository.UsuarioRepository;
import com.smartlogix.auth.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(String username, String password) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);

        if (usuarioOpt.isEmpty() || !usuarioOpt.get().isActivo()
                || !passwordEncoder.matches(password, usuarioOpt.get().getPasswordHash())) {
            log.warn("Intento de login fallido para usuario: {}", username);
            return new LoginResponse(false, "Usuario o contraseña inválidos");
        }

        Usuario usuario = usuarioOpt.get();
        String token = jwtUtil.generateToken(usuario.getUsername(), usuario.getRole());
        log.info("Login exitoso para: {} [{}]", usuario.getUsername(), usuario.getRole());

        return new LoginResponse(true, "Login exitoso", token, usuario.getUsername(), usuario.getRole());
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
    // ── Gestión de usuarios (Admin) ─────────────────────────────────────────────
    public List<UsuarioDTO> listarUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public UsuarioDTO crearUsuario(UsuarioDTO dto) {
        if (usuarioRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("El usuario ya existe: " + dto.getUsername());
        }
        Usuario usuario = Usuario.builder()
                .username(dto.getUsername())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .role(dto.getRole())
                .activo(true)
                .build();
        return toDTO(usuarioRepository.save(usuario));
    }

    /** Registro público — siempre crea rol USER, sin importar lo que mande el cliente. */
    public UsuarioDTO registrarUsuario(UsuarioDTO dto) {
        if (dto.getUsername() == null || dto.getUsername().isBlank()
                || dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new IllegalArgumentException("Usuario y contraseña son obligatorios.");
        }
        if (usuarioRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("El usuario ya existe: " + dto.getUsername());
        }
        Usuario usuario = Usuario.builder()
                .username(dto.getUsername())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .role("USER") // forzado, ignora cualquier role que venga en el DTO
                .activo(true)
                .build();
        return toDTO(usuarioRepository.save(usuario));
    }

    public UsuarioDTO actualizarUsuario(Long id, UsuarioDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));

        if (dto.getRole() != null) {
            usuario.setRole(dto.getRole());
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        usuario.setActivo(dto.isActivo());

        return toDTO(usuarioRepository.save(usuario));
    }

    public void eliminarUsuario(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new IllegalArgumentException("Usuario no encontrado: " + id);
        }
        usuarioRepository.deleteById(id);
    }

    private UsuarioDTO toDTO(Usuario usuario) {
        return UsuarioDTO.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .role(usuario.getRole())
                .activo(usuario.isActivo())
                .build();
    }
}
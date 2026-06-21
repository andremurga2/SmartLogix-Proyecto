package com.smartlogix.auth.service;

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
}
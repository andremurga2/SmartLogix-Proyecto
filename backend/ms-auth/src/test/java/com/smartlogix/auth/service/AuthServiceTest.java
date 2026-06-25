package com.smartlogix.auth.service;

import com.smartlogix.auth.model.LoginResponse;
import com.smartlogix.auth.model.UsuarioDTO;
import com.smartlogix.auth.model.ValidateResponse;
import com.smartlogix.auth.model.entity.Usuario;
import com.smartlogix.auth.repository.UsuarioRepository;
import com.smartlogix.auth.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private Usuario usuarioMock;

    @BeforeEach
    void setUp() {
        usuarioMock = Usuario.builder()
                .id(1L)
                .username("admin")
                .passwordHash("$2a$hashed")
                .role("ADMIN")
                .activo(true)
                .build();
    }

    // ── LOGIN ──────────────────────────────────────────────────────────────────

    @Test
    void debeRetornarTokenCuandoLoginEsExitoso() {
        // Arrange
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("password123", "$2a$hashed")).thenReturn(true);
        when(jwtUtil.generateToken("admin", "ADMIN")).thenReturn("jwt-token-fake");

        // Act
        LoginResponse response = authService.login("admin", "password123");

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("jwt-token-fake", response.getToken());
        assertEquals("admin", response.getUsername());
        verify(jwtUtil, times(1)).generateToken("admin", "ADMIN");
    }

    @Test
    void debeRetornarFalsoCuandoPasswordEsIncorrecto() {
        // Arrange
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("wrong", "$2a$hashed")).thenReturn(false);

        // Act
        LoginResponse response = authService.login("admin", "wrong");

        // Assert
        assertFalse(response.isSuccess());
        assertNull(response.getToken());
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    void debeRetornarFalsoCuandoUsuarioNoExiste() {
        // Arrange
        when(usuarioRepository.findByUsername("noexiste")).thenReturn(Optional.empty());

        // Act
        LoginResponse response = authService.login("noexiste", "cualquiera");

        // Assert
        assertFalse(response.isSuccess());
    }

    @Test
    void debeRetornarFalsoCuandoUsuarioEstaInactivo() {
        // Arrange
        usuarioMock.setActivo(false);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuarioMock));

        // Act
        LoginResponse response = authService.login("admin", "password123");

        // Assert
        assertFalse(response.isSuccess());
    }

    // ── VALIDATE ───────────────────────────────────────────────────────────────

    @Test
    void debeRetornarInvalidoCuandoTokenNullOSinBearer() {
        ValidateResponse r1 = authService.validate(null);
        ValidateResponse r2 = authService.validate("sin-prefijo");

        assertFalse(r1.isValid());
        assertFalse(r2.isValid());
    }

    // ── REGISTRO ───────────────────────────────────────────────────────────────

    @Test
    void debeRegistrarUsuarioNuevoConRolUser() {
        // Arrange
        UsuarioDTO dto = new UsuarioDTO();
        dto.setUsername("nuevo");
        dto.setPassword("pass123");
        dto.setRole("ADMIN"); // intenta poner ADMIN — debe ignorarse

        when(usuarioRepository.findByUsername("nuevo")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass123")).thenReturn("$2a$hashed_nuevo");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });

        // Act
        UsuarioDTO resultado = authService.registrarUsuario(dto);

        // Assert
        assertEquals("USER", resultado.getRole()); // el rol ADMIN fue ignorado
        assertEquals("nuevo", resultado.getUsername());
    }

    @Test
    void debeLanzarExcepcionSiUsernameYaExiste() {
        // Arrange
        UsuarioDTO dto = new UsuarioDTO();
        dto.setUsername("admin");
        dto.setPassword("pass");
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuarioMock));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authService.registrarUsuario(dto));
    }

    @Test
    void debeLanzarExcepcionSiUsernameEsBlanco() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setUsername("  ");
        dto.setPassword("pass");

        assertThrows(IllegalArgumentException.class, () -> authService.registrarUsuario(dto));
    }
}
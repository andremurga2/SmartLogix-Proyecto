package com.smartlogix.auth.service;

import com.smartlogix.auth.model.LoginResponse;
import com.smartlogix.auth.model.UsuarioDTO;
import com.smartlogix.auth.model.ValidateResponse;
import com.smartlogix.auth.model.entity.Usuario;
import com.smartlogix.auth.repository.UsuarioRepository;
import com.smartlogix.auth.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private Usuario usuarioMock;

    @BeforeEach
    void setUp() {
        usuarioMock = Usuario.builder()
                .id(1L).username("admin").passwordHash("$2a$hashed")
                .role("ADMIN").activo(true).build();
    }

    // ── LOGIN ──────────────────────────────────────────────────────────────────

    @Test
    void debeRetornarTokenCuandoLoginEsExitoso() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("password123", "$2a$hashed")).thenReturn(true);
        when(jwtUtil.generateToken("admin", "ADMIN")).thenReturn("jwt-token-fake");

        LoginResponse response = authService.login("admin", "password123");

        assertTrue(response.isSuccess());
        assertEquals("jwt-token-fake", response.getToken());
        assertEquals("admin", response.getUsername());
        verify(jwtUtil, times(1)).generateToken("admin", "ADMIN");
    }

    @Test
    void debeRetornarFalsoCuandoPasswordEsIncorrecto() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("wrong", "$2a$hashed")).thenReturn(false);

        LoginResponse response = authService.login("admin", "wrong");

        assertFalse(response.isSuccess());
        assertNull(response.getToken());
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    void debeRetornarFalsoCuandoUsuarioNoExiste() {
        when(usuarioRepository.findByUsername("noexiste")).thenReturn(Optional.empty());

        LoginResponse response = authService.login("noexiste", "cualquiera");

        assertFalse(response.isSuccess());
    }

    @Test
    void debeRetornarFalsoCuandoUsuarioEstaInactivo() {
        usuarioMock.setActivo(false);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuarioMock));

        LoginResponse response = authService.login("admin", "password123");

        assertFalse(response.isSuccess());
    }

    // ── VALIDATE ───────────────────────────────────────────────────────────────

    @Test
    void debeRetornarInvalidoCuandoTokenNullOSinBearer() {
        assertFalse(authService.validate(null).isValid());
        assertFalse(authService.validate("sin-prefijo").isValid());
    }

    @Test
    void debeRetornarInvalidoCuandoTokenNoEsValido() {
        when(jwtUtil.isTokenValid("token-expirado")).thenReturn(false);

        ValidateResponse r = authService.validate("Bearer token-expirado");

        assertFalse(r.isValid());
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    void debeRetornarValidoCuandoTokenEsCorrecto() {
        Claims claimsMock = mock(Claims.class);
        when(claimsMock.getSubject()).thenReturn("admin");
        when(claimsMock.get("role", String.class)).thenReturn("ADMIN");

        when(jwtUtil.isTokenValid("token-bueno")).thenReturn(true);
        when(jwtUtil.validateToken("token-bueno")).thenReturn(claimsMock);

        ValidateResponse r = authService.validate("Bearer token-bueno");

        assertTrue(r.isValid());
        assertEquals("admin", r.getUsername());
        assertEquals("ADMIN", r.getRole());
    }

    // ── REGISTRO ───────────────────────────────────────────────────────────────

    @Test
    void debeRegistrarUsuarioNuevoConRolUser() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setUsername("nuevo");
        dto.setPassword("pass123");
        dto.setRole("ADMIN"); // debe ser ignorado

        when(usuarioRepository.findByUsername("nuevo")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass123")).thenReturn("$2a$hashed_nuevo");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });

        UsuarioDTO resultado = authService.registrarUsuario(dto);

        assertEquals("USER", resultado.getRole());
        assertEquals("nuevo", resultado.getUsername());
    }

    @Test
    void debeLanzarExcepcionSiUsernameYaExiste() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setUsername("admin");
        dto.setPassword("pass");
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuarioMock));

        assertThrows(IllegalArgumentException.class, () -> authService.registrarUsuario(dto));
    }

    @Test
    void debeLanzarExcepcionSiUsernameEsBlanco() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setUsername("  ");
        dto.setPassword("pass");

        assertThrows(IllegalArgumentException.class, () -> authService.registrarUsuario(dto));
    }

    @Test
    void debeLanzarExcepcionSiPasswordEsBlanco() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setUsername("user");
        dto.setPassword("");

        assertThrows(IllegalArgumentException.class, () -> authService.registrarUsuario(dto));
    }

    // ── CREAR USUARIO (ADMIN) ─────────────────────────────────────────────────

    @Test
    void debeCrearUsuarioCuandoNoExiste() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setUsername("nuevo2");
        dto.setPassword("pass");
        dto.setRole("ADMIN");

        when(usuarioRepository.findByUsername("nuevo2")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass")).thenReturn("hashed");
        when(usuarioRepository.save(any())).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(3L);
            return u;
        });

        UsuarioDTO result = authService.crearUsuario(dto);

        assertEquals("nuevo2", result.getUsername());
        assertEquals("ADMIN", result.getRole());
    }

    @Test
    void debeLanzarExcepcionAlCrearUsuarioQueYaExiste() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setUsername("admin");
        dto.setPassword("pass");

        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuarioMock));

        assertThrows(IllegalArgumentException.class, () -> authService.crearUsuario(dto));
    }

    // ── LISTAR USUARIOS ───────────────────────────────────────────────────────

    @Test
    void debeListarTodosLosUsuarios() {
        when(usuarioRepository.findAll()).thenReturn(List.of(usuarioMock));

        List<UsuarioDTO> lista = authService.listarUsuarios();

        assertEquals(1, lista.size());
        assertEquals("admin", lista.get(0).getUsername());
    }

    // ── ACTUALIZAR USUARIO ────────────────────────────────────────────────────

    @Test
    void debeActualizarRolYEstadoDeUsuario() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setRole("USER");
        dto.setPassword("nuevaPass");
        dto.setActivo(false);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.encode("nuevaPass")).thenReturn("hashed_nueva");
        when(usuarioRepository.save(any())).thenReturn(usuarioMock);

        UsuarioDTO result = authService.actualizarUsuario(1L, dto);

        assertNotNull(result);
        verify(usuarioRepository, times(1)).save(usuarioMock);
    }

    @Test
    void debeActualizarSinCambiarPasswordSiEsBlanco() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setRole("USER");
        dto.setPassword(null); // sin cambio de password
        dto.setActivo(true);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(usuarioRepository.save(any())).thenReturn(usuarioMock);

        authService.actualizarUsuario(1L, dto);

        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void debeLanzarExcepcionAlActualizarUsuarioNoExistente() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> authService.actualizarUsuario(99L, new UsuarioDTO()));
    }

    // ── ELIMINAR USUARIO ──────────────────────────────────────────────────────

    @Test
    void debeEliminarUsuarioExistente() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);

        authService.eliminarUsuario(1L);

        verify(usuarioRepository, times(1)).deleteById(1L);
    }

    @Test
    void debeLanzarExcepcionAlEliminarUsuarioNoExistente() {
        when(usuarioRepository.existsById(99L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.eliminarUsuario(99L));
    }
}
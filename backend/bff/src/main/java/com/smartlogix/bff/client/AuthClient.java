package com.smartlogix.bff.client;

import com.smartlogix.bff.model.UsuarioDTO;
import com.smartlogix.bff.model.ValidateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "ms-auth", url = "${smartlogix.auth.url}")
public interface AuthClient {

    @GetMapping("/api/auth/validate")
    ValidateResponse validate(@RequestHeader("Authorization") String bearerToken);

    @GetMapping("/api/auth/usuarios")
    List<UsuarioDTO> listarUsuarios(@RequestHeader("Authorization") String bearerToken);

    @PostMapping("/api/auth/registro")
    UsuarioDTO registrarUsuario(@RequestBody UsuarioDTO dto);

    @PostMapping("/api/auth/usuarios")
    UsuarioDTO crearUsuario(@RequestHeader("Authorization") String bearerToken, @RequestBody UsuarioDTO dto);

    @PutMapping("/api/auth/usuarios/{id}")
    UsuarioDTO actualizarUsuario(@RequestHeader("Authorization") String bearerToken, @PathVariable("id") Long id, @RequestBody UsuarioDTO dto);

    @DeleteMapping("/api/auth/usuarios/{id}")
    void eliminarUsuario(@RequestHeader("Authorization") String bearerToken, @PathVariable("id") Long id);
}
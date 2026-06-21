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
    List<UsuarioDTO> listarUsuarios();

    @PostMapping("/api/auth/registro")
    UsuarioDTO registrarUsuario(@RequestBody UsuarioDTO dto);

    @PostMapping("/api/auth/usuarios")
    UsuarioDTO crearUsuario(@RequestBody UsuarioDTO dto);

    @PutMapping("/api/auth/usuarios/{id}")
    UsuarioDTO actualizarUsuario(@PathVariable("id") Long id, @RequestBody UsuarioDTO dto);

    @DeleteMapping("/api/auth/usuarios/{id}")
    void eliminarUsuario(@PathVariable("id") Long id);
}
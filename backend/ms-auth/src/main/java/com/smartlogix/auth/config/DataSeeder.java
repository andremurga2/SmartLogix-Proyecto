package com.smartlogix.auth.config;

import com.smartlogix.auth.model.entity.Usuario;
import com.smartlogix.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Carga usuarios de prueba si la tabla está vacía.
 * En producción esto se reemplaza por un endpoint de registro o un script de migración.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            System.out.println("Base de datos de usuarios vacía. Cargando usuarios de prueba...");

            Usuario admin = Usuario.builder()
                    .username("admin")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role("ADMIN")
                    .activo(true)
                    .build();

            Usuario user1 = Usuario.builder()
                    .username("user1")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .role("USER")
                    .activo(true)
                    .build();

            Usuario user2 = Usuario.builder()
                    .username("user2")
                    .passwordHash(passwordEncoder.encode("password456"))
                    .role("USER")
                    .activo(true)
                    .build();

            usuarioRepository.saveAll(java.util.List.of(admin, user1, user2));
            System.out.println("¡Usuarios de prueba cargados exitosamente!");
        }
    }
}
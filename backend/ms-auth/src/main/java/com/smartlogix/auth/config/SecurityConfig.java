package com.smartlogix.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ms-auth emite y valida JWT propios (ver JwtUtil/AuthService).
 * No usamos el modelo de sesión/login-form de Spring Security aquí,
 * solo el BCryptPasswordEncoder. Por eso se desactiva CSRF, sesiones
 * y se permite el acceso público a todos los endpoints de este servicio.
 *
 * Si en el futuro se agregan endpoints que deban validar el JWT con
 * Spring Security (filtros, @PreAuthorize, etc.), este es el lugar
 * para registrar ese filtro.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable());

        return http.build();
    }
}
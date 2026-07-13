package com.smartlogix.auth.security;

import com.smartlogix.auth.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Valida el JWT directamente dentro de ms-auth (defensa en profundidad).
 * Antes, ms-auth confiaba en que solo el BFF llegaba hasta acá; con este
 * filtro, cualquier request que llegue al puerto 8083 con un token
 * inválido o sin token queda como anónimo, y SecurityConfig decide
 * si esa ruta requiere autenticación.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtUtil.validateToken(token);
                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                var authority = new SimpleGrantedAuthority("ROLE_" + role);
                var authToken = new UsernamePasswordAuthenticationToken(
                        username, null, List.of(authority));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (Exception ex) {
                // Token inválido o expirado: se deja el contexto sin autenticar.
                // SecurityConfig decide si la ruta solicitada requiere auth.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
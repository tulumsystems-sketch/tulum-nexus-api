package com.tulumcore.api.security;

import com.tulumcore.api.config.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class TenantFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. BYPASS DE CORS: Dejamos pasar las peticiones OPTIONS sin preguntar nada
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // 2. BYPASS DE RUTAS PÚBLICAS
        if (path.startsWith("/api/auth") || path.startsWith("/api/webhook")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        // 3. VALIDACIÓN DE PRESENCIA DE TOKEN
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Token requerido para esta operacion\"}");
            return; // Cortamos la ejecución acá
        }

        // 4. LECTURA DEL TOKEN (Protegido con Try-Catch SOLO para el token)
        try {
            String jwt = authHeader.substring(7);
            Claims claims = jwtService.extractAllClaims(jwt);
            String userEmail = claims.getSubject();
            String tenantId = claims.get("tenant_id", String.class);

            if (userEmail != null && tenantId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                TenantContext.setCurrentTenant(tenantId);
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userEmail, null, Collections.emptyList()
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Token invalido o expirado\"}");
            return; // Cortamos la ejecución si el token es trucho
        }

        // 5. EJECUCIÓN DEL CONTROLADOR (Fuera del try-catch del token)
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Limpiamos siempre el Tenant al terminar la petición
            TenantContext.clear();
        }
    }
}
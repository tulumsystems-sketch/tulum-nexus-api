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

        // 1. BYPASS DE CORS
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // 2. BYPASS DE RUTAS PÚBLICAS Y EXTERNAS (n8n / Bot)
        if (path.startsWith("/api/auth") || path.startsWith("/api/webhook") || path.startsWith("/api/external")) {

            // Si es una ruta externa, intentamos setear el Tenant desde el Header directo
            if (path.startsWith("/api/external")) {
                String tenantIdHeader = request.getHeader("X-Tenant-ID");
                if (tenantIdHeader != null && !tenantIdHeader.isEmpty()) {
                    TenantContext.setCurrentTenant(tenantIdHeader);

                    // Creamos una autenticación ficticia de "Sistema" para que Spring Security no rebote
                    UsernamePasswordAuthenticationToken systemAuth = new UsernamePasswordAuthenticationToken(
                            "SYSTEM_BOT", null, Collections.emptyList()
                    );
                    SecurityContextHolder.getContext().setAuthentication(systemAuth);
                } else {
                    // Si es external pero no mandan el ID del local, rebotamos
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"X-Tenant-ID requerido para peticiones externas\"}");
                    return;
                }
            }

            try {
                filterChain.doFilter(request, response);
            } finally {
                TenantContext.clear();
            }
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        // 3. VALIDACIÓN DE PRESENCIA DE TOKEN (Solo para el resto de la App)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Token requerido para esta operacion\"}");
            return;
        }

        // 4. LECTURA DEL TOKEN JWT
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
            return;
        }

        // 5. EJECUCIÓN DEL CONTROLADOR
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
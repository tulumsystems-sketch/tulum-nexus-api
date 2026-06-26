package com.tulumcore.api.security;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.repositories.TenantConfigRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class TenantFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TenantConfigRepository tenantConfigRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        TenantContext.clear();
        SecurityContextHolder.clearContext();

        // 1. BYPASS DE CORS
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
            response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Tenant-ID, Origin, Accept");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String path = request.getRequestURI();

        // 2. BYPASS DE RUTAS PÚBLICAS Y EXTERNAS
        if (path.startsWith("/api/auth") || path.startsWith("/api/webhook") || path.startsWith("/api/external")) {
            if (path.startsWith("/api/external")) {
                String tenantIdHeader = request.getHeader("X-Tenant-ID");
                if (tenantIdHeader != null && !tenantIdHeader.isEmpty()) {
                    TenantContext.setCurrentTenant(tenantIdHeader);
                }

                boolean tenantValido = tenantIdHeader != null && !tenantIdHeader.isEmpty()
                        && tenantConfigRepository.findByTenantId(tenantIdHeader)
                        .filter(config -> config.isActivo())
                        .isPresent();

                if (tenantValido) {
                    UsernamePasswordAuthenticationToken systemAuth = new UsernamePasswordAuthenticationToken(
                            "SYSTEM_BOT", null, Collections.emptyList()
                    );
                    SecurityContextHolder.getContext().setAuthentication(systemAuth);
                } else {
                    TenantContext.clear();
                    SecurityContextHolder.clearContext();
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"X-Tenant-ID invalido para peticiones externas\"}");
                    return;
                }
            }

            try {
                filterChain.doFilter(request, response);
            } finally {
                TenantContext.clear();
                SecurityContextHolder.clearContext();
            }
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        // 3. VALIDACIÓN DE PRESENCIA DE TOKEN
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
            String rol = claims.get("rol", String.class);

            if (userEmail != null && tenantId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                TenantContext.setCurrentTenant(tenantId);

                // ← FIX CRÍTICO: incluir el rol como authority para que Spring Security
                // pueda evaluar hasRole("ADMIN") correctamente
                String authority = "ROLE_" + (rol != null ? rol : "OPERADOR");
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userEmail, null, List.of(new SimpleGrantedAuthority(authority))
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
            SecurityContextHolder.clearContext();
        }
    }
}

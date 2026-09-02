package com.tulumcore.api.security;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.FeatureKey;
import com.tulumcore.api.entities.TenantConfig;
import com.tulumcore.api.repositories.TenantConfigRepository;
import com.tulumcore.api.services.TenantFeatureService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;

@Component
public class TenantFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TenantConfigRepository tenantConfigRepository;

    @Autowired
    private TenantFeatureService tenantFeatureService;

    @Value("${app.bot.shared-secret:}")
    private String botSharedSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        TenantContext.clear();
        SecurityContextHolder.clearContext();

        // 1. BYPASS DE CORS
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
            response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With, X-Tenant-ID, X-Bot-Secret, Origin, Accept");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Vary", "Origin");
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String path = request.getRequestURI();

        // 2. BYPASS DE RUTAS PÚBLICAS Y EXTERNAS
        if (path.equals("/health") || path.equals("/api/health")
                || path.startsWith("/api/auth") || path.startsWith("/api/webhook") || path.startsWith("/api/external")) {
            if (path.startsWith("/api/external")) {
                if (!autenticarBot(request, response)) {
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
                // Hay que setear el tenant ANTES de leer tenant_config: Hibernate @TenantId
                // filtra por el contexto actual (si está vacío usa "public" y el comercio "no existe").
                TenantContext.setCurrentTenant(tenantId);

                if (!comercioActivo(tenantId, rol)) {
                    TenantContext.clear();
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Comercio inactivo\"}");
                    return;
                }

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

    /**
     * El bot queda apagado si no hay secreto, si el header no coincide,
     * si el tenant no está activo o si WHATSAPP_BOT está deshabilitado.
     */
    private boolean autenticarBot(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String secretoEnviado = request.getHeader("X-Bot-Secret");
        if (!secretoBotValido(secretoEnviado)) {
            responderJson(response, HttpServletResponse.SC_UNAUTHORIZED, "Bot no autorizado");
            return false;
        }

        String tenantIdHeader = request.getHeader("X-Tenant-ID");
        if (tenantIdHeader == null || tenantIdHeader.isEmpty()) {
            responderJson(response, HttpServletResponse.SC_BAD_REQUEST, "X-Tenant-ID invalido para peticiones externas");
            return false;
        }

        TenantContext.setCurrentTenant(tenantIdHeader);
        boolean tenantValido = tenantConfigRepository.findByTenantId(tenantIdHeader)
                .filter(TenantConfig::isActivo)
                .isPresent();
        if (!tenantValido) {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
            responderJson(response, HttpServletResponse.SC_BAD_REQUEST, "X-Tenant-ID invalido para peticiones externas");
            return false;
        }

        if (!tenantFeatureService.isEnabled(FeatureKey.WHATSAPP_BOT)) {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
            responderJson(response, HttpServletResponse.SC_FORBIDDEN, "Bot WhatsApp no habilitado para este comercio");
            return false;
        }

        UsernamePasswordAuthenticationToken systemAuth = new UsernamePasswordAuthenticationToken(
                "SYSTEM_BOT", null, Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(systemAuth);
        return true;
    }

    private boolean secretoBotValido(String header) {
        if (!StringUtils.hasText(botSharedSecret) || header == null) {
            return false;
        }
        byte[] esperado = botSharedSecret.getBytes(StandardCharsets.UTF_8);
        byte[] enviado = header.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(esperado, enviado);
    }

    private boolean comercioActivo(String tenantId, String rol) {
        if ("SUPER_ADMIN".equals(rol)) {
            return true;
        }
        return tenantConfigRepository.findByTenantId(tenantId)
                .map(TenantConfig::isActivo)
                .orElse(false);
    }

    private void responderJson(HttpServletResponse response, int status, String mensaje) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + mensaje + "\"}");
    }
}

package com.tulumcore.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.security.Key;

@Service
public class JwtService {
    // IMPORTANTE: Esta clave debe ser de al menos 256 bits (32 caracteres) y estar en el application.properties en producción.
    private static final String SECRET_KEY = "TulumApexSecretKeyParaEntornoDeDesarrollo123!";

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractTenantId(String token) {
        // Asumimos que al crear el JWT, le pusimos un claim llamado "tenant_id"
        return extractAllClaims(token).get("tenant_id", String.class);
    }

    // Acá luego podés agregar extractUsername, isTokenValid, etc.
    public String generateToken(String username, String tenantId) {
        Map<String, Object> claims = new HashMap<>();
        // Inyectamos el tenantId como un claim personalizado
        claims.put("tenant_id", tenantId);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                // Configuramos la expiración (ejemplo: 24 horas de validez)
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
                .signWith(getSigningKey())
                .compact();
    }
}
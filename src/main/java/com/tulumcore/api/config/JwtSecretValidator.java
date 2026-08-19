package com.tulumcore.api.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Si JWT_SECRET es el de ejemplo, cualquiera puede firmar tokens.
 * La API no arranca en ese estado.
 */
@Component
public class JwtSecretValidator {

    static final String SECRETO_DE_EJEMPLO = "change-me-change-me-change-me-change-me-change-me";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    public void validar() {
        if (!StringUtils.hasText(jwtSecret)
                || jwtSecret.equals(SECRETO_DE_EJEMPLO)
                || jwtSecret.toLowerCase().contains("change-me")
                || jwtSecret.length() < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET invalido. Definir un secreto largo y unico. La API no arranca con el valor de ejemplo."
            );
        }
    }
}

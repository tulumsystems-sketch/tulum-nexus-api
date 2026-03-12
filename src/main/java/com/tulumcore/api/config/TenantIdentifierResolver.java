package com.tulumcore.api.config;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return tenantId;
        }
        // Fallback obligatorio para que Spring Boot pueda arrancar
        // y para endpoints públicos que no requieren login.
        return "DEFAULT_TENANT";
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
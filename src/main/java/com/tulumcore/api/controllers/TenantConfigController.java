package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.TenantConfig;
import com.tulumcore.api.repositories.TenantConfigRepository;
import com.tulumcore.api.config.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
public class TenantConfigController {

    @Autowired
    private TenantConfigRepository configRepository;

    @GetMapping
    public TenantConfig getConfig() {
        String tenant = TenantContext.getCurrentTenant();
        // Si no existe configuración para este tenant, le creamos una vacía en memoria para el Front
        return configRepository.findByTenantId(tenant)
                .orElseGet(TenantConfig::new);
    }

    @PostMapping
    public TenantConfig updateConfig(@RequestBody TenantConfig dto) {
        String tenant = TenantContext.getCurrentTenant();

        // Buscamos si ya tiene configuración, si no, creamos una nueva
        TenantConfig config = configRepository.findByTenantId(tenant)
                .orElse(new TenantConfig());
        config.setTenantId(tenant);

        // Actualizamos los valores
        config.setNombreEmpresa(dto.getNombreEmpresa());
        config.setLogoUrl(dto.getLogoUrl());
        config.setMpAccessToken(dto.getMpAccessToken());
        config.setMpAceptarCredito(dto.isMpAceptarCredito());
        config.setMpAceptarDebito(dto.isMpAceptarDebito());
        config.setMpAceptarEfectivo(dto.isMpAceptarEfectivo());
        config.setClientesHabilitado(dto.isClientesHabilitado());
        config.setRemitosHabilitado(dto.isRemitosHabilitado());

        return configRepository.save(config);
    }
}

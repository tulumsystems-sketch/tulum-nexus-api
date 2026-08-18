package com.tulumcore.api.controllers;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.TenantConfig;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.repositories.TenantConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
public class TenantConfigController {

    @Autowired
    private TenantConfigRepository configRepository;

    @GetMapping
    public TenantConfigResponseDTO getConfig() {
        return TenantConfigResponseDTO.from(obtenerOCrearConfig());
    }

    @PostMapping
    @Transactional
    public TenantConfigResponseDTO updateConfig(@RequestBody TenantConfigUpdateDTO dto) {
        TenantConfig config = obtenerOCrearConfig();

        // null = no tocar. Los wrappers Boolean sí distinguen false de "no vino".
        if (dto.getNombreEmpresa() != null) config.setNombreEmpresa(dto.getNombreEmpresa());
        if (dto.getLogoUrl() != null) config.setLogoUrl(dto.getLogoUrl());
        if (dto.getMpAccessToken() != null && !dto.getMpAccessToken().isBlank()) {
            config.setMpAccessToken(dto.getMpAccessToken());
        }
        if (dto.getMpAceptarCredito() != null) config.setMpAceptarCredito(dto.getMpAceptarCredito());
        if (dto.getMpAceptarDebito() != null) config.setMpAceptarDebito(dto.getMpAceptarDebito());
        if (dto.getMpAceptarEfectivo() != null) config.setMpAceptarEfectivo(dto.getMpAceptarEfectivo());
        if (dto.getClientesHabilitado() != null) config.setClientesHabilitado(dto.getClientesHabilitado());
        if (dto.getRemitosHabilitado() != null) config.setRemitosHabilitado(dto.getRemitosHabilitado());
        if (dto.getComprasHabilitado() != null) config.setComprasHabilitado(dto.getComprasHabilitado());
        if (dto.getStockHabilitado() != null) config.setStockHabilitado(dto.getStockHabilitado());
        if (dto.getPagoEfectivoHabilitado() != null) config.setPagoEfectivoHabilitado(dto.getPagoEfectivoHabilitado());
        if (dto.getPagoTransferenciaHabilitado() != null) {
            config.setPagoTransferenciaHabilitado(dto.getPagoTransferenciaHabilitado());
        }
        if (dto.getPagoMercadoPagoHabilitado() != null) {
            config.setPagoMercadoPagoHabilitado(dto.getPagoMercadoPagoHabilitado());
        }
        if (dto.getAliasCobro() != null) config.setAliasCobro(dto.getAliasCobro());
        if (dto.getIvaPorcentaje() != null) config.setIvaPorcentaje(dto.getIvaPorcentaje());
        if (Boolean.TRUE.equals(dto.getLimpiarMargenPorDefecto())) {
            config.setMargenPorDefecto(null);
        } else if (dto.getMargenPorDefecto() != null) {
            config.setMargenPorDefecto(dto.getMargenPorDefecto());
        }

        return TenantConfigResponseDTO.from(configRepository.save(config));
    }

    private TenantConfig obtenerOCrearConfig() {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null || tenant.isBlank()) {
            throw new BusinessException("No hay tenant en la sesion. Volve a iniciar sesion.");
        }
        return configRepository.findByTenantId(tenant).orElseGet(() -> {
            TenantConfig nuevo = new TenantConfig();
            nuevo.setTenantId(tenant);
            return nuevo;
        });
    }
}

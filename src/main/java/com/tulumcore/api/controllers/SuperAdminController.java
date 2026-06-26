package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.TenantConfig;
import com.tulumcore.api.entities.Usuario;
import com.tulumcore.api.services.SuperAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class SuperAdminController {

    @Autowired
    private SuperAdminService service;

    @GetMapping("/tenants")
    public List<TenantConfig> listarTenants() {
        return service.listarConfigs();
    }

    @PutMapping("/tenants/{tenantId}/status")
    public ResponseEntity<Void> toggleStatus(@PathVariable String tenantId, @RequestBody Map<String, Boolean> body) {
        boolean activo = body.getOrDefault("activo", true);
        service.toggleTenantStatus(tenantId, activo);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tenants/{tenantId}/config")
    public TenantConfig getConfig(@PathVariable String tenantId) {
        return service.getConfig(tenantId);
    }

    @PutMapping("/tenants/{tenantId}/config")
    public TenantConfig updateConfig(@PathVariable String tenantId, @RequestBody TenantConfig dto) {
        return service.updateConfig(tenantId, dto);
    }

    @GetMapping("/tenants/{tenantId}/usuarios")
    public List<Map<String, Object>> listarUsuarios(@PathVariable String tenantId) {
        return service.listarUsuarios(tenantId).stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "email", u.getEmail(),
                        "rol", u.getRol().name()
                ))
                .toList();
    }
}

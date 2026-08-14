package com.tulumcore.api.controllers;

import com.tulumcore.api.services.TenantFeatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class TenantFeatureController {

    @Autowired
    private TenantFeatureService service;

    @GetMapping("/api/features/me")
    public Map<String, Boolean> getMyFeatures() {
        return service.listCurrentTenantFeatureStates()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().name(),
                        Map.Entry::getValue
                ));
    }

    @GetMapping("/api/superadmin/tenants/{tenantId}/features")
    public java.util.List<TenantFeatureDTO> getTenantFeatures(@PathVariable String tenantId) {
        return service.listForTenantAsSuperAdmin(tenantId);
    }

    @PutMapping("/api/superadmin/tenants/{tenantId}/features/{featureKey}")
    public TenantFeatureDTO updateTenantFeature(
            @PathVariable String tenantId,
            @PathVariable String featureKey,
            @RequestBody TenantFeatureUpdateDTO dto
    ) {
        return service.updateForTenantAsSuperAdmin(tenantId, featureKey, dto);
    }
}

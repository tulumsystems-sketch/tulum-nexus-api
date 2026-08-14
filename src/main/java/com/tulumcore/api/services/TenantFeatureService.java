package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.controllers.TenantFeatureDTO;
import com.tulumcore.api.controllers.TenantFeatureUpdateDTO;
import com.tulumcore.api.entities.FeatureKey;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.exceptions.FeatureDisabledException;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.repositories.TenantFeatureRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TenantFeatureService {

    @Autowired
    private TenantFeatureRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    public boolean isEnabled(FeatureKey featureKey) {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null || tenant.isBlank()) {
            return false;
        }
        return repository.findByTenantIdAndFeatureKey(tenant, featureKey)
                .map(feature -> feature.isEnabled())
                .orElse(false);
    }

    public void requireEnabled(FeatureKey featureKey) {
        if (!isEnabled(featureKey)) {
            throw new FeatureDisabledException(featureKey);
        }
    }

    public Map<FeatureKey, Boolean> listCurrentTenantFeatureStates() {
        String tenant = TenantContext.getCurrentTenant();
        Map<FeatureKey, Boolean> states = defaultFeatureStates();
        if (tenant == null || tenant.isBlank()) {
            return states;
        }

        repository.findAllByTenantId(tenant).forEach(feature ->
                states.put(feature.getFeatureKey(), feature.isEnabled())
        );
        return states;
    }

    public List<TenantFeatureDTO> listForTenantAsSuperAdmin(String tenantId) {
        ensureTenantExists(tenantId);
        Map<FeatureKey, TenantFeatureDTO> result = Arrays.stream(FeatureKey.values())
                .collect(Collectors.toMap(
                        featureKey -> featureKey,
                        featureKey -> new TenantFeatureDTO(featureKey.name(), false, null),
                        (left, right) -> left,
                        () -> new EnumMap<>(FeatureKey.class)
                ));

        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT feature_key, enabled, configuration_json
                        FROM tenant_features
                        WHERE tenant_id = :tenantId
                        """)
                .setParameter("tenantId", tenantId)
                .getResultList();

        for (Object[] row : rows) {
            FeatureKey featureKey = parseFeatureKey((String) row[0]);
            result.put(featureKey, new TenantFeatureDTO(
                    featureKey.name(),
                    Boolean.TRUE.equals(row[1]),
                    (String) row[2]
            ));
        }

        return Arrays.stream(FeatureKey.values())
                .map(result::get)
                .toList();
    }

    @Transactional
    public TenantFeatureDTO updateForTenantAsSuperAdmin(
            String tenantId,
            String rawFeatureKey,
            TenantFeatureUpdateDTO dto
    ) {
        ensureTenantExists(tenantId);
        FeatureKey featureKey = parseFeatureKey(rawFeatureKey);
        if (dto == null || dto.enabled() == null) {
            throw new BusinessException("El campo enabled es obligatorio.");
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = entityManager.createNativeQuery("""
                        UPDATE tenant_features
                        SET enabled = :enabled,
                            configuration_json = :configurationJson,
                            updated_at = :updatedAt
                        WHERE tenant_id = :tenantId
                          AND feature_key = :featureKey
                        """)
                .setParameter("enabled", dto.enabled())
                .setParameter("configurationJson", dto.configurationJson())
                .setParameter("updatedAt", now)
                .setParameter("tenantId", tenantId)
                .setParameter("featureKey", featureKey.name())
                .executeUpdate();

        if (updated == 0) {
            entityManager.createNativeQuery("""
                            INSERT INTO tenant_features (
                                tenant_id, feature_key, enabled, configuration_json, created_at, updated_at
                            )
                            VALUES (
                                :tenantId, :featureKey, :enabled, :configurationJson, :createdAt, :updatedAt
                            )
                            """)
                    .setParameter("tenantId", tenantId)
                    .setParameter("featureKey", featureKey.name())
                    .setParameter("enabled", dto.enabled())
                    .setParameter("configurationJson", dto.configurationJson())
                    .setParameter("createdAt", now)
                    .setParameter("updatedAt", now)
                    .executeUpdate();
        }

        return new TenantFeatureDTO(featureKey.name(), dto.enabled(), dto.configurationJson());
    }

    private Map<FeatureKey, Boolean> defaultFeatureStates() {
        Map<FeatureKey, Boolean> states = new EnumMap<>(FeatureKey.class);
        for (FeatureKey featureKey : FeatureKey.values()) {
            states.put(featureKey, false);
        }
        return states;
    }

    private FeatureKey parseFeatureKey(String rawFeatureKey) {
        try {
            return FeatureKey.valueOf(rawFeatureKey);
        } catch (Exception ex) {
            throw new BusinessException("Feature key invalida: " + rawFeatureKey);
        }
    }

    private void ensureTenantExists(String tenantId) {
        Number count = (Number) entityManager.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM (
                            SELECT tenant_id FROM tenant_config WHERE tenant_id = :tenantId
                            UNION
                            SELECT tenant_id FROM usuarios WHERE tenant_id = :tenantId
                        ) tenants
                        """)
                .setParameter("tenantId", tenantId)
                .getSingleResult();

        if (count.longValue() == 0) {
            throw new ResourceNotFoundException("Tenant no encontrado: " + tenantId);
        }
    }
}

package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.FeatureKey;
import com.tulumcore.api.entities.TenantFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantFeatureRepository extends JpaRepository<TenantFeature, Long> {
    Optional<TenantFeature> findByTenantIdAndFeatureKey(String tenantId, FeatureKey featureKey);
    List<TenantFeature> findAllByTenantId(String tenantId);
}

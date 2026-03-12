package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.TenantConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TenantConfigRepository extends JpaRepository<TenantConfig, Long> {
    Optional<TenantConfig> findByTenantId(String tenantId);
}
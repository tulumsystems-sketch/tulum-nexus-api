package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.TenantConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TenantConfigRepository extends JpaRepository<TenantConfig, Long> {
    Optional<TenantConfig> findByTenantId(String tenantId);

    /**
     * Native: Hibernate @TenantId filtra tenant_config al tenant del hilo.
     * Sin contexto (login/filtro) el fallback es "public" y findByTenantId no ve a Chirino.
     */
    @Query(value = "SELECT activo FROM tenant_config WHERE tenant_id = :tenantId LIMIT 1", nativeQuery = true)
    Boolean findActivoNativo(@Param("tenantId") String tenantId);
}
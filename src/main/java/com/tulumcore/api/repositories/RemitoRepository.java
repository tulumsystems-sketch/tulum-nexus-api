package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.Remito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RemitoRepository extends JpaRepository<Remito, Long> {

    List<Remito> findAllByTenantIdOrderByFechaDesc(String tenantId);

    List<Remito> findAllByTenantIdAndEstadoOrderByFechaDesc(String tenantId, String estado);

    List<Remito> findByTenantIdAndEstadoIn(String tenantId, List<String> estados);

    Optional<Remito> findByIdAndTenantId(Long id, String tenantId);
}
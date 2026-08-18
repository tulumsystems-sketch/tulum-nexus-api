package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.PagoRemito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagoRemitoRepository extends JpaRepository<PagoRemito, Long> {

    List<PagoRemito> findAllByTenantIdOrderByFechaDesc(String tenantId);

    List<PagoRemito> findAllByTenantIdAndRemitoIdOrderByFechaDesc(String tenantId, Long remitoId);

    Optional<PagoRemito> findByIdAndTenantId(Long id, String tenantId);
}

package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.PagoRemito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PagoRemitoRepository extends JpaRepository<PagoRemito, Long> {

    List<PagoRemito> findAllByTenantIdOrderByFechaDesc(String tenantId);

    List<PagoRemito> findAllByTenantIdAndRemitoIdOrderByFechaDesc(String tenantId, Long remitoId);

    List<PagoRemito> findAllByTenantIdAndFechaGreaterThanEqual(String tenantId, LocalDateTime desde);

    Optional<PagoRemito> findByIdAndTenantId(Long id, String tenantId);
}

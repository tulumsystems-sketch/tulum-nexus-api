package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {
    List<Venta> findByTenantId(String currentTenant);
    List<Venta> findByTenantIdAndFechaAfter(String tenantId, LocalDateTime desde);
}
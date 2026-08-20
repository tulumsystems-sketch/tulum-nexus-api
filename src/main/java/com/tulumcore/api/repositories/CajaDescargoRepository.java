package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.CajaDescargo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CajaDescargoRepository extends JpaRepository<CajaDescargo, Long> {
    List<CajaDescargo> findAllByTenantIdAndCajaIdOrderByFechaDesc(String tenantId, Long cajaId);
}

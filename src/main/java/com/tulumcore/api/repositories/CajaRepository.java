package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.Caja;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CajaRepository extends JpaRepository<Caja, Long> {
    // Buscamos la caja que esté abierta para el tenant actual
    Optional<Caja> findByEstadoAndTenantId(String estado, String tenantId);
}
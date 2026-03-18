package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.Caja;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CajaRepository extends JpaRepository<Caja, Long> {

    // Para validar si puede vender o cerrar
    Optional<Caja> findByEstadoAndTenantId(String estado, String tenantId);

    // Para el historial de auditoría
    List<Caja> findAllByTenantIdOrderByFechaAperturaDesc(String tenantId);
}
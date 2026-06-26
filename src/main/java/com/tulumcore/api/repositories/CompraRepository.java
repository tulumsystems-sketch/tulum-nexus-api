package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.Compra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    List<Compra> findAllByTenantIdOrderByFechaDesc(String tenantId);

    Optional<Compra> findByIdAndTenantId(Long id, String tenantId);
}

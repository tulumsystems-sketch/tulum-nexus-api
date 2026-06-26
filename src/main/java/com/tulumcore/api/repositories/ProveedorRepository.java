package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    List<Proveedor> findAllByTenantId(String tenantId);

    Optional<Proveedor> findByIdAndTenantId(Long id, String tenantId);
}

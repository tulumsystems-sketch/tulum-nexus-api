package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findAllByTenantId(String tenantId);

    Optional<Producto> findByIdAndTenantId(Long id, String tenantId);

    List<Producto> findByNombreContainingIgnoreCaseAndTenantId(String nombre, String tenantId);

    Optional<Producto> findByCodigoBarrasAndTenantId(String codigoBarras, String tenantId);
}

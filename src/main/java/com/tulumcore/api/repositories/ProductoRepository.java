package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.Producto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findAllByTenantId(String tenantId);

    Optional<Producto> findByIdAndTenantId(Long id, String tenantId);

    List<Producto> findByNombreContainingIgnoreCaseAndTenantId(String nombre, String tenantId);

    Optional<Producto> findByCodigoBarrasAndTenantId(String codigoBarras, String tenantId);

    @Query("""
            SELECT p FROM Producto p
            LEFT JOIN p.categoria c
            WHERE p.tenantId = :tenant
              AND (
                LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(p.descripcion, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(p.medidas, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(c.nombre, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            ORDER BY p.nombre
            """)
    List<Producto> buscarCatalogo(@Param("tenant") String tenant, @Param("q") String q, Pageable pageable);
}

package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.ProductoReceta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ProductoRecetaRepository extends JpaRepository<ProductoReceta, Long> {
    List<ProductoReceta> findByProductoIdAndTenantIdOrderByIdAsc(Long productoId, String tenantId);

    @Modifying
    @Transactional
    void deleteByProductoIdAndTenantId(Long productoId, String tenantId);

    boolean existsByInsumoIdAndTenantId(Long insumoId, String tenantId);

    boolean existsByProductoIdAndTenantId(Long productoId, String tenantId);
}

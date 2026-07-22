package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.Producto;
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
            select p
            from Producto p
            where p.tenantId = :tenantId
              and (
                lower(p.nombre) like lower(concat('%', :query, '%'))
                or p.codigoBarras like concat('%', :query, '%')
              )
            """)
    List<Producto> buscarPorNombreOCodigo(@Param("query") String query, @Param("tenantId") String tenantId);
}

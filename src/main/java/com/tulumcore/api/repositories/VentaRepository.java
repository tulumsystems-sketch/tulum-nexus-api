package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {
    List<Venta> findByTenantId(String currentTenant);
    List<Venta> findByTenantIdAndFechaAfter(String tenantId, LocalDateTime desde);
    List<Venta> findByTenantIdAndFechaGreaterThanEqual(String tenantId, LocalDateTime desde);
    Optional<Venta> findByIdAndTenantId(Long id, String tenantId);
    List<Venta> findByClienteIdAndTenantIdOrderByFechaDesc(Long clienteId, String tenantId);

    @Query("""
            SELECT DISTINCT v FROM Venta v
            LEFT JOIN FETCH v.cliente
            LEFT JOIN FETCH v.items i
            LEFT JOIN FETCH i.producto p
            LEFT JOIN FETCH p.categoria
            WHERE v.id = :id AND v.tenantId = :tenant
            """)
    Optional<Venta> findDetalleByIdAndTenantId(@Param("id") Long id, @Param("tenant") String tenant);

    @Query("""
            SELECT DISTINCT v FROM Venta v
            LEFT JOIN FETCH v.cliente
            LEFT JOIN FETCH v.items i
            LEFT JOIN FETCH i.producto
            WHERE v.id IN :ids
            """)
    List<Venta> findWithItemsByIdIn(@Param("ids") List<Long> ids);

    @Query("""
            SELECT COUNT(v), COALESCE(SUM(v.totalFinal), 0)
            FROM Venta v
            WHERE v.tenantId = :tenant
              AND (v.estado IS NULL OR v.estado <> 'ANULADA')
            """)
    List<Object[]> totalesNoAnuladas(@Param("tenant") String tenant);
}

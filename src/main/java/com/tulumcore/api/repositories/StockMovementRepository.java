package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.MovementType;
import com.tulumcore.api.entities.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findAllByTenantIdOrderByFechaDesc(String tenantId);

    List<StockMovement> findAllByTenantIdAndProductoIdOrderByFechaDesc(String tenantId, Long productoId);

    Optional<StockMovement> findByIdAndTenantId(Long id, String tenantId);

    List<StockMovement> findFirst10ByTenantIdOrderByFechaDesc(String tenantId);

    @Query("SELECT s FROM StockMovement s WHERE s.tenantId = :tenantId " +
           "AND (:tipo IS NULL OR s.tipoMovimiento = :tipo) " +
           "AND (:desde IS NULL OR s.fecha >= :desde) " +
           "AND (:hasta IS NULL OR s.fecha <= :hasta) " +
           "ORDER BY s.fecha DESC")
    List<StockMovement> buscarPorFiltros(
            @Param("tenantId") String tenantId,
            @Param("tipo") MovementType tipo,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}

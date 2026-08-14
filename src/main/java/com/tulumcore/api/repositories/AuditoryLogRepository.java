package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.AuditoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditoryLogRepository extends JpaRepository<AuditoryLog, Long> {

    List<AuditoryLog> findAllByTenantIdOrderByFechaDesc(String tenantId);

    @Query("SELECT a FROM AuditoryLog a WHERE a.tenantId = :tenantId " +
           "AND (:entidad IS NULL OR a.entidad = :entidad) " +
           "AND (:accion IS NULL OR a.accion = :accion) " +
           "AND (:desde IS NULL OR a.fecha >= :desde) " +
           "AND (:hasta IS NULL OR a.fecha <= :hasta) " +
           "ORDER BY a.fecha DESC")
    List<AuditoryLog> buscarPorFiltros(
            @Param("tenantId") String tenantId,
            @Param("entidad") String entidad,
            @Param("accion") String accion,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}

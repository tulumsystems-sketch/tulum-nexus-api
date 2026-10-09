package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TurnoRepository extends JpaRepository<Turno, Long> {

    Optional<Turno> findByIdAndTenantId(Long id, String tenantId);

    List<Turno> findByTenantIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
            String tenantId, Instant desde, Instant hasta
    );

    @Query("""
            SELECT t FROM Turno t
            WHERE t.tenantId = :tenant
              AND t.estado NOT IN ('CANCELADO', 'AUSENTE')
              AND t.inicio < :fin
              AND t.fin > :inicio
              AND (:excludeId IS NULL OR t.id <> :excludeId)
            """)
    List<Turno> solapados(
            @Param("tenant") String tenant,
            @Param("inicio") Instant inicio,
            @Param("fin") Instant fin,
            @Param("excludeId") Long excludeId
    );

    List<Turno> findByTenantIdAndCliente_IdOrderByInicioDesc(String tenantId, Long clienteId);

    @Query("""
            SELECT t FROM Turno t
            JOIN FETCH t.cliente c
            WHERE t.tenantId = :tenant
              AND t.estado IN ('PEDIDO', 'CONFIRMADO')
              AND t.inicio >= :desde
              AND t.inicio < :hasta
            ORDER BY t.inicio
            """)
    List<Turno> recordatoriosVentana(
            @Param("tenant") String tenant,
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta
    );
}

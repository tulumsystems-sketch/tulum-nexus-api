package com.tulumcore.api.jobs;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.services.CajaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class CajaCierreAutomaticoJob {

    private static final Logger log = LoggerFactory.getLogger(CajaCierreAutomaticoJob.class);

    private final JdbcTemplate jdbcTemplate;
    private final CajaService cajaService;

    @Value("${app.caja.max-horas-abierta:24}")
    private int maxHorasAbierta;

    public CajaCierreAutomaticoJob(JdbcTemplate jdbcTemplate, CajaService cajaService) {
        this.jdbcTemplate = jdbcTemplate;
        this.cajaService = cajaService;
    }

    /**
     * Recorre cajas abiertas de todos los tenants (JDBC nativo: Hibernate filtra por @TenantId).
     * Cada cierre corre con el tenant de esa caja.
     */
    @Scheduled(fixedDelayString = "${app.caja.cierre-automatico-ms:300000}")
    public void cerrarTurnosVencidos() {
        Timestamp limite = Timestamp.valueOf(LocalDateTime.now().minusMinutes((long) maxHorasAbierta * 60));
        List<CajaAbiertaRef> vencidas = jdbcTemplate.query(
                "SELECT id, tenant_id FROM cajas WHERE estado = 'ABIERTA' AND fecha_apertura <= ?",
                (rs, rowNum) -> new CajaAbiertaRef(rs.getLong("id"), rs.getString("tenant_id")),
                limite
        );
        if (vencidas.isEmpty()) {
            return;
        }
        log.info("Cerrando {} caja(s) vencida(s) automaticamente", vencidas.size());
        for (CajaAbiertaRef ref : vencidas) {
            try {
                TenantContext.setCurrentTenant(ref.tenantId());
                cajaService.cerrarTurnoAutomaticoPorId(ref.id());
            } catch (Exception e) {
                log.error("No se pudo cerrar automaticamente la caja {} del tenant {}", ref.id(), ref.tenantId(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    private record CajaAbiertaRef(long id, String tenantId) {}
}

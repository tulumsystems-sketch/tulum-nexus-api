package com.tulumcore.api.services;

import com.tulumcore.api.entities.Caja;
import com.tulumcore.api.entities.Usuario;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.repositories.CajaRepository;
import com.tulumcore.api.config.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CajaService {

    @Autowired
    private CajaRepository cajaRepository;

    @Autowired
    private AuditoryLogService auditoryLogService;

    public Optional<Caja> obtenerCajaAbierta() {
        String tenant = TenantContext.getCurrentTenant();
        return cajaRepository.findByEstadoAndTenantId("ABIERTA", tenant);
    }

    public List<Caja> obtenerHistorial() {
        String tenant = TenantContext.getCurrentTenant();
        return cajaRepository.findAllByTenantIdOrderByFechaAperturaDesc(tenant);
    }

    @Transactional
    public Caja abrirCaja(Double montoInicial) {
        String tenant = TenantContext.getCurrentTenant();
        Usuario usuario = auditoryLogService.getCurrentUser();

        if (obtenerCajaAbierta().isPresent()) {
            throw new BusinessException("Ya existe una caja abierta para este comercio.");
        }

        Caja nuevaCaja = new Caja();
        nuevaCaja.setFechaApertura(LocalDateTime.now());
        nuevaCaja.setMontoInicial(montoInicial);
        nuevaCaja.setMontoVentasEfectivo(0.0);
        nuevaCaja.setMontoVentasMP(0.0);
        nuevaCaja.setMontoVentasTransferencia(0.0);
        nuevaCaja.setMontoCobranzasEfectivo(0.0);
        nuevaCaja.setMontoCobranzasTransferencia(0.0);
        nuevaCaja.setMontoFinalEsperado(montoInicial);
        nuevaCaja.setEstado("ABIERTA");
        nuevaCaja.setTenantId(tenant);
        nuevaCaja.setUsuarioApertura(usuario);

        Caja saved = cajaRepository.save(nuevaCaja);
        auditoryLogService.registrar("CREATE", "CAJA", saved.getId(),
                "Caja abierta", null, detalleCaja(saved));
        return saved;
    }

    @Transactional
    public Caja cerrarCaja(Double montoFinalReal) {
        Caja caja = obtenerCajaAbierta()
                .orElseThrow(() -> new ResourceNotFoundException("No hay una caja abierta para cerrar."));

        String detalleAnterior = detalleCaja(caja);
        caja.setFechaCierre(LocalDateTime.now());
        caja.setMontoFinalReal(montoFinalReal);
        caja.setEstado("CERRADA");

        Caja saved = cajaRepository.save(caja);
        auditoryLogService.registrar("UPDATE", "CAJA", saved.getId(),
                "Caja cerrada", detalleAnterior, detalleCaja(saved));
        return saved;
    }

    /**
     * Recalcula el efectivo esperado en el cajon: monto inicial + ventas en efectivo
     * + cobranzas de remitos en efectivo. Las transferencias y Mercado Pago quedan afuera.
     */
    public double recalcularMontoFinalEsperado(Caja caja) {
        double esperado = nz(caja.getMontoInicial())
                + nz(caja.getMontoVentasEfectivo())
                + nz(caja.getMontoCobranzasEfectivo());
        caja.setMontoFinalEsperado(esperado);
        return esperado;
    }

    private double nz(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private String detalleCaja(Caja caja) {
        return auditoryLogService.detalle(
                "estado", caja.getEstado(),
                "montoInicial", caja.getMontoInicial(),
                "montoVentasEfectivo", caja.getMontoVentasEfectivo(),
                "montoVentasMP", caja.getMontoVentasMP(),
                "montoVentasTransferencia", caja.getMontoVentasTransferencia(),
                "montoCobranzasEfectivo", caja.getMontoCobranzasEfectivo(),
                "montoCobranzasTransferencia", caja.getMontoCobranzasTransferencia(),
                "montoFinalEsperado", caja.getMontoFinalEsperado(),
                "montoFinalReal", caja.getMontoFinalReal()
        );
    }
}

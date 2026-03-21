package com.tulumcore.api.services;

import com.tulumcore.api.entities.Caja;
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

        if (obtenerCajaAbierta().isPresent()) {
            throw new BusinessException("Ya existe una caja abierta para este comercio.");
        }

        Caja nuevaCaja = new Caja();
        nuevaCaja.setFechaApertura(LocalDateTime.now());
        nuevaCaja.setMontoInicial(montoInicial);
        nuevaCaja.setMontoVentasEfectivo(0.0);
        nuevaCaja.setMontoVentasMP(0.0);
        nuevaCaja.setMontoFinalEsperado(montoInicial);
        nuevaCaja.setEstado("ABIERTA");
        nuevaCaja.setTenantId(tenant);

        return cajaRepository.save(nuevaCaja);
    }

    @Transactional
    public Caja cerrarCaja(Double montoFinalReal) {
        Caja caja = obtenerCajaAbierta()
                .orElseThrow(() -> new ResourceNotFoundException("No hay una caja abierta para cerrar."));

        caja.setFechaCierre(LocalDateTime.now());
        caja.setMontoFinalReal(montoFinalReal);
        caja.setEstado("CERRADA");

        return cajaRepository.save(caja);
    }
}
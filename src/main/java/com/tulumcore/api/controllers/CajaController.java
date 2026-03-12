package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.Caja;
import com.tulumcore.api.repositories.CajaRepository;
import com.tulumcore.api.config.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/caja")
public class CajaController {

    @Autowired
    private CajaRepository cajaRepository;

    @GetMapping("/estado")
    public ResponseEntity<?> getEstadoCaja() {
        String tenant = TenantContext.getCurrentTenant();
        Optional<Caja> cajaAbierta = cajaRepository.findByEstadoAndTenantId("ABIERTA", tenant);
        return ResponseEntity.ok(cajaAbierta.orElse(null));
    }

    @PostMapping("/apertura")
    public ResponseEntity<?> abrirCaja(@RequestBody CajaAperturaDTO dto) {
        String tenant = TenantContext.getCurrentTenant();
        if (cajaRepository.findByEstadoAndTenantId("ABIERTA", tenant).isPresent()) {
            return ResponseEntity.badRequest().body("Ya existe una caja abierta.");
        }

        Caja nuevaCaja = new Caja();
        nuevaCaja.setFechaApertura(LocalDateTime.now());
        nuevaCaja.setMontoInicial(dto.getMontoInicial());
        nuevaCaja.setMontoVentasEfectivo(0.0);
        nuevaCaja.setMontoVentasMP(0.0);
        nuevaCaja.setMontoFinalEsperado(dto.getMontoInicial());
        nuevaCaja.setEstado("ABIERTA");

        return ResponseEntity.ok(cajaRepository.save(nuevaCaja));
    }

    @PostMapping("/cierre")
    public ResponseEntity<?> cerrarCaja(@RequestBody Double montoFinalReal) {
        String tenant = TenantContext.getCurrentTenant();

        Caja caja = cajaRepository.findByEstadoAndTenantId("ABIERTA", tenant)
                .orElseThrow(() -> new RuntimeException("No hay una caja abierta para cerrar."));

        caja.setFechaCierre(LocalDateTime.now());
        caja.setMontoFinalReal(montoFinalReal);
        caja.setEstado("CERRADA");

        // Aquí el backend ya tiene:
        // 1. Lo que había al inicio.
        // 2. Lo que se vendió (gracias a VentaService).
        // 3. Lo que el cajero dice que hay ahora.

        return ResponseEntity.ok(cajaRepository.save(caja));
    }
}
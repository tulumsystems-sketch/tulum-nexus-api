package com.tulumcore.api.controllers;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.Caja;
import com.tulumcore.api.services.CajaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/caja")
public class CajaController {

    @Autowired
    private CajaService cajaService;

    @GetMapping("/estado")
    public ResponseEntity<?> getEstadoCaja() {
        return ResponseEntity.ok(cajaService.obtenerCajaAbierta().orElse(null));
    }

    @PostMapping("/apertura")
    public ResponseEntity<?> abrirCaja(@RequestBody CajaAperturaDTO dto) {
        try {
            Caja caja = cajaService.abrirCaja(dto.getMontoInicial());
            return ResponseEntity.ok(caja);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/cierre")
    public ResponseEntity<?> cerrarCaja(@RequestBody CajaCierreDTO dto) { // <--- Cambiamos Double por DTO
        try {
            Caja caja = cajaService.cerrarCaja(dto.getMontoFinalReal());
            return ResponseEntity.ok(caja);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/historial")
    public ResponseEntity<List<Caja>> getHistorialCajas() {
        return ResponseEntity.ok(cajaService.obtenerHistorial());    }
}
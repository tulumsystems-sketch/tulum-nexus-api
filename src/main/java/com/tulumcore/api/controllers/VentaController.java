package com.tulumcore.api.controllers;

import com.tulumcore.api.controllers.VentaDTO;
import com.tulumcore.api.controllers.VentaResumenDTO;
import com.tulumcore.api.entities.Venta;
import com.tulumcore.api.services.VentaService;
import com.tulumcore.api.config.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    @Autowired private VentaService ventaService;

    @PostMapping
    public ResponseEntity<?> crearVenta(@RequestBody VentaDTO dto) {
        try {
            return ResponseEntity.ok(ventaService.guardar(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Venta>> filtrarVentas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String metodoPago,
            @RequestParam(required = false) String estado,
            Pageable pageable) {
        String tenant = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(ventaService.buscarVentas(tenant, desde, hasta, metodoPago, estado, pageable));
    }

    @GetMapping("/stats/resumen-semanal")
    public ResponseEntity<List<VentaResumenDTO>> getResumenSemanal() {
        return ResponseEntity.ok(ventaService.obtenerResumenSemanal(TenantContext.getCurrentTenant()));
    }
}
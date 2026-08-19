package com.tulumcore.api.controllers;

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
        return ResponseEntity.ok(ventaService.guardar(dto));
    }

    /** Listado liviano: sin ítems ni productos. El dashboard no debe pedir el grafo completo. */
    @GetMapping
    public ResponseEntity<List<VentaListadoDTO>> getAllVentas() {
        String tenant = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(ventaService.getAllVentas(tenant));
    }

    @PutMapping("/{id}/anular")
    public ResponseEntity<Venta> anularVenta(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.anularVenta(id));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<VentaListadoDTO>> filtrarVentas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String metodoPago,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false, defaultValue = "false") boolean whatsapp,
            Pageable pageable) {
        String tenant = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(ventaService.buscarVentas(
                tenant, desde, hasta, metodoPago, estado, clienteId, whatsapp, pageable));
    }

    @GetMapping("/stats/resumen-semanal")
    public ResponseEntity<List<VentaResumenDTO>> getResumenSemanal() {
        return ResponseEntity.ok(ventaService.obtenerResumenSemanal(TenantContext.getCurrentTenant()));
    }

    @GetMapping("/stats/resumen-hoy")
    public ResponseEntity<VentaResumenDTO> getResumenHoy() {
        return ResponseEntity.ok(ventaService.obtenerResumenHoy(TenantContext.getCurrentTenant()));
    }

    @GetMapping("/stats/totales")
    public ResponseEntity<VentaTotalesDTO> getTotales() {
        return ResponseEntity.ok(ventaService.obtenerTotales(TenantContext.getCurrentTenant()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Venta> getVenta(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.obtenerDetalle(id));
    }
}

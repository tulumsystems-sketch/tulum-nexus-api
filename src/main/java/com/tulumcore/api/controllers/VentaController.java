package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.Venta;
import com.tulumcore.api.repositories.VentaRepository;
import com.tulumcore.api.services.VentaService;
import com.tulumcore.api.config.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    @Autowired private VentaRepository ventaRepository;
    @Autowired private VentaService ventaService;

    @GetMapping
    public List<Venta> getAllVentas() {
        return ventaRepository.findByTenantId(TenantContext.getCurrentTenant());
    }

    @PostMapping
    public ResponseEntity<?> crearVenta(@RequestBody VentaDTO dto) {
        try {
            Venta nuevaVenta = ventaService.guardar(dto);
            return ResponseEntity.ok(nuevaVenta);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"message\": \"" + e.getMessage() + "\"}");
        }
    }

    @PutMapping("/{id}/anular")
    public ResponseEntity<?> anularVenta(@PathVariable Long id) {
        try {
            Venta ventaAnulada = ventaService.anularVenta(id);
            return ResponseEntity.ok(ventaAnulada);
        } catch (Exception e) {
            return ResponseEntity.status(400).body("{\"message\": \"" + e.getMessage() + "\"}");
        }
    }
}
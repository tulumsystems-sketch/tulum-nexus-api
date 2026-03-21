package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.Remito;
import com.tulumcore.api.services.RemitoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/remitos")
public class RemitoController {

    @Autowired
    private RemitoService remitoService;

    @GetMapping
    public List<Remito> getAll() {
        return remitoService.getAll();
    }

    @GetMapping("/estado/{estado}")
    public List<Remito> getByEstado(@PathVariable String estado) {
        return remitoService.getByEstado(estado);
    }

    @PostMapping
    public ResponseEntity<Remito> crear(@RequestBody RemitoDTO dto) {
        return ResponseEntity.ok(remitoService.crear(dto));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<Remito> cambiarEstado(
            @PathVariable Long id,
            @RequestParam String estado) {
        return ResponseEntity.ok(remitoService.cambiarEstado(id, estado));
    }
}
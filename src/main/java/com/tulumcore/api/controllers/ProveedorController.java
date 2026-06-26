package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.Proveedor;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.services.ProveedorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
public class ProveedorController {

    @Autowired
    private ProveedorService service;

    @GetMapping
    public List<Proveedor> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Proveedor getById(@PathVariable Long id) {
        return service.getById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con id: " + id));
    }

    @PostMapping
    public Proveedor create(@RequestBody Proveedor proveedor) {
        return service.save(proveedor);
    }

    @PutMapping("/{id}")
    public Proveedor update(@PathVariable Long id, @RequestBody Proveedor dto) {
        Proveedor existente = service.getById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con id: " + id));
        existente.setNombre(dto.getNombre());
        existente.setContacto(dto.getContacto());
        existente.setTelefono(dto.getTelefono());
        existente.setEmail(dto.getEmail());
        existente.setDireccion(dto.getDireccion());
        existente.setCuit(dto.getCuit());
        existente.setObservaciones(dto.getObservaciones());
        return service.save(existente);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.getById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con id: " + id));
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

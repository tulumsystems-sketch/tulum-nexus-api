package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.Compra;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.services.CompraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compras")
public class CompraController {

    @Autowired
    private CompraService compraService;

    @GetMapping
    public List<Compra> getAll() {
        return compraService.getAll();
    }

    @PostMapping
    public Compra create(@RequestBody CompraDTO dto) {
        return compraService.crear(dto);
    }

    @PutMapping("/{id}/recibir")
    public Compra recibir(@PathVariable Long id) {
        return compraService.recibirMercaderia(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        compraService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

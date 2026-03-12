package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.Categoria;
import com.tulumcore.api.services.CategoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {
    @Autowired
    private CategoriaService categoriaService;

    @GetMapping
    public List<CategoriaDTO> getAllCategorias() {
        return categoriaService.getAllCategorias().stream()
                .map(categoria -> {
                    CategoriaDTO dto = new CategoriaDTO();
                    dto.setId(categoria.getId());
                    dto.setNombre(categoria.getNombre());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaDTO> getCategoriaById(@PathVariable Long id) {
        return categoriaService.getCategoriaById(id)
                .map(categoria -> {
                    CategoriaDTO dto = new CategoriaDTO();
                    dto.setId(categoria.getId());
                    dto.setNombre(categoria.getNombre());
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CategoriaDTO> createCategoria(@RequestBody Categoria categoria) {
        if (categoria.getNombre() == null || categoria.getNombre().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(null);  // Devuelve un error si el nombre es nulo o vacío
        }
        Categoria createdCategoria = categoriaService.createOrUpdateCategoria(categoria);
        CategoriaDTO dto = new CategoriaDTO();
        dto.setId(createdCategoria.getId());
        dto.setNombre(createdCategoria.getNombre());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaDTO> updateCategoria(@PathVariable Long id, @RequestBody Categoria categoria) {
        return categoriaService.getCategoriaById(id)
                .map(existingCategoria -> {
                    categoria.setId(id);
                    Categoria updatedCategoria = categoriaService.createOrUpdateCategoria(categoria);
                    CategoriaDTO dto = new CategoriaDTO();
                    dto.setId(updatedCategoria.getId());
                    dto.setNombre(updatedCategoria.getNombre());
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    @GetMapping("/latest")
    public List<CategoriaDTO> getLatestCategorias(@RequestParam(defaultValue = "5") int limit) {
        return categoriaService.getLatestCategorias(limit).stream()
                .map(categoria -> {
                    CategoriaDTO dto = new CategoriaDTO();
                    dto.setId(categoria.getId());
                    dto.setNombre(categoria.getNombre());
                    return dto;
                })
                .collect(Collectors.toList());
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategoria(@PathVariable Long id) {
        if (categoriaService.getCategoriaById(id).isPresent()) {
            categoriaService.deleteCategoria(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

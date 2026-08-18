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

    /** Unidad usada cuando la categoría no declara ninguna (categorías creadas antes del campo). */
    private static final String UNIDAD_POR_DEFECTO = "UNIDAD";

    @Autowired
    private CategoriaService categoriaService;

    @GetMapping
    public List<CategoriaDTO> getAllCategorias() {
        return categoriaService.getAllCategorias().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaDTO> getCategoriaById(@PathVariable Long id) {
        return categoriaService.getCategoriaById(id)
                .map(categoria -> ResponseEntity.ok(toDTO(categoria)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CategoriaDTO> createCategoria(@RequestBody Categoria categoria) {
        if (categoria.getNombre() == null || categoria.getNombre().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(null);  // Devuelve un error si el nombre es nulo o vacío
        }
        categoria.setUnidadMedida(normalizarUnidad(categoria.getUnidadMedida(), UNIDAD_POR_DEFECTO));
        Categoria createdCategoria = categoriaService.createOrUpdateCategoria(categoria);
        return ResponseEntity.ok(toDTO(createdCategoria));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaDTO> updateCategoria(@PathVariable Long id, @RequestBody Categoria categoria) {
        return categoriaService.getCategoriaById(id)
                .map(existingCategoria -> {
                    if (categoria.getNombre() != null && !categoria.getNombre().trim().isEmpty()) {
                        existingCategoria.setNombre(categoria.getNombre().trim());
                    }
                    existingCategoria.setUnidadMedida(normalizarUnidad(
                            categoria.getUnidadMedida(),
                            normalizarUnidad(existingCategoria.getUnidadMedida(), UNIDAD_POR_DEFECTO)));
                    Categoria updatedCategoria = categoriaService.createOrUpdateCategoria(existingCategoria);
                    return ResponseEntity.ok(toDTO(updatedCategoria));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/latest")
    public List<CategoriaDTO> getLatestCategorias(@RequestParam(defaultValue = "5") int limit) {
        return categoriaService.getLatestCategorias(limit).stream()
                .map(this::toDTO)
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

    // =============================================
    // Mapper privado: entidad → DTO
    // =============================================
    private CategoriaDTO toDTO(Categoria categoria) {
        CategoriaDTO dto = new CategoriaDTO();
        dto.setId(categoria.getId());
        dto.setNombre(categoria.getNombre());
        dto.setUnidadMedida(normalizarUnidad(categoria.getUnidadMedida(), UNIDAD_POR_DEFECTO));
        return dto;
    }

    private String normalizarUnidad(String unidad, String porDefecto) {
        if (unidad == null || unidad.trim().isEmpty()) {
            return porDefecto;
        }
        return unidad.trim().toUpperCase();
    }
}

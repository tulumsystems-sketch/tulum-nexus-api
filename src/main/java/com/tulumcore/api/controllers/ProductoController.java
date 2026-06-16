package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.Categoria;
import com.tulumcore.api.entities.Producto;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.services.CategoriaService;
import com.tulumcore.api.services.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private CategoriaService categoriaService;

    @GetMapping
    public List<ProductoResponseDTO> getAllProductos() {
        return productoService.getAllProductos()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ProductoResponseDTO> createProducto(@RequestBody ProductoDTO dto) {
        if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new BusinessException("El nombre del producto es obligatorio");
        }

        Categoria categoria = categoriaService.getCategoriaById(dto.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + dto.getCategoriaId()));

        Producto producto = new Producto();
        producto.setNombre(dto.getNombre());
        producto.setDescripcion(dto.getDescripcion());
        producto.setPrecio(dto.getPrecio());
        producto.setCantidadStock(dto.getCantidadStock());
        producto.setStockMinimo(dto.getStockMinimo());
        producto.setMedidas(dto.getMedidas());
        producto.setImageUrl(dto.getImageUrl());
        producto.setCategoria(categoria);

        return ResponseEntity.ok(toDTO(productoService.createOrUpdateProducto(producto)));
    }

    @PutMapping("/{id}")
    public ProductoResponseDTO updateProducto(@PathVariable Long id, @RequestBody ProductoDTO dto) {
        Producto existente = productoService.getProductoById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));

        existente.setNombre(dto.getNombre());
        existente.setDescripcion(dto.getDescripcion());
        existente.setPrecio(dto.getPrecio());
        existente.setCantidadStock(dto.getCantidadStock());
        existente.setStockMinimo(dto.getStockMinimo());
        existente.setMedidas(dto.getMedidas());

        if (dto.getImageUrl() != null) {
            existente.setImageUrl(dto.getImageUrl());
        }

        if (dto.getCategoriaId() != null) {
            Categoria categoria = categoriaService.getCategoriaById(dto.getCategoriaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + dto.getCategoriaId()));
            existente.setCategoria(categoria);
        }

        return toDTO(productoService.createOrUpdateProducto(existente));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProducto(@PathVariable Long id) {
        productoService.getProductoById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        productoService.deleteProducto(id);
        return ResponseEntity.noContent().build();
    }

    // =============================================
    // Mapper privado: entidad → DTO
    // =============================================
    private ProductoResponseDTO toDTO(Producto p) {
        CategoriaDTO categoriaDTO = null;
        if (p.getCategoria() != null) {
            categoriaDTO = new CategoriaDTO();
            categoriaDTO.setId(p.getCategoria().getId());
            categoriaDTO.setNombre(p.getCategoria().getNombre());
        }

        return new ProductoResponseDTO(
                p.getId(),
                p.getNombre(),
                p.getDescripcion(),
                p.getPrecio(),
                p.getCantidadStock(),
                p.getStockMinimo(),
                p.getMedidas(),
                p.getImageUrl(),
                categoriaDTO
        );
    }
}
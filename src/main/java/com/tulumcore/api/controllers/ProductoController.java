package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.Categoria;
import com.tulumcore.api.entities.Producto;
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
    public List<Producto> getAllProductos() {
        return productoService.getAllProductos();
    }

    @PostMapping
    public ResponseEntity<?> createProducto(@RequestBody ProductoDTO productoDTO) {
        if (productoDTO.getNombre() == null || productoDTO.getNombre().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("El nombre del producto es obligatorio");
        }

        Categoria categoria = categoriaService.getCategoriaById(productoDTO.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        Producto producto = new Producto();
        producto.setNombre(productoDTO.getNombre());
        producto.setDescripcion(productoDTO.getDescripcion());
        producto.setPrecio(productoDTO.getPrecio());
        producto.setCantidadStock(productoDTO.getCantidadStock());
        producto.setMedidas(productoDTO.getMedidas());
        producto.setImageUrl(productoDTO.getImageUrl()); // <-- Mapeo de imagen
        producto.setCategoria(categoria);

        Producto createdProducto = productoService.createOrUpdateProducto(producto);
        return ResponseEntity.ok(createdProducto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Producto> updateProducto(@PathVariable Long id, @RequestBody ProductoDTO productoDTO) {
        return productoService.getProductoById(id)
                .map(existingProducto -> {
                    existingProducto.setNombre(productoDTO.getNombre());
                    existingProducto.setDescripcion(productoDTO.getDescripcion());
                    existingProducto.setPrecio(productoDTO.getPrecio());
                    existingProducto.setCantidadStock(productoDTO.getCantidadStock());
                    existingProducto.setMedidas(productoDTO.getMedidas());

                    // Solo actualizamos la imagen si el DTO trae una (nueva o vieja)
                    if (productoDTO.getImageUrl() != null) {
                        existingProducto.setImageUrl(productoDTO.getImageUrl());
                    }

                    if (productoDTO.getCategoriaId() != null) {
                        Categoria categoria = categoriaService.getCategoriaById(productoDTO.getCategoriaId()).orElse(null);
                        existingProducto.setCategoria(categoria);
                    }

                    Producto updatedProducto = productoService.createOrUpdateProducto(existingProducto);
                    return ResponseEntity.ok(updatedProducto);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProducto(@PathVariable Long id) {
        if (productoService.getProductoById(id).isPresent()) {
            productoService.deleteProducto(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
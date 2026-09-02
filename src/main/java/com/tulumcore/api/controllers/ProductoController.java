package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.Categoria;
import com.tulumcore.api.entities.MovementType;
import com.tulumcore.api.entities.Producto;
import com.tulumcore.api.entities.ProductoTipo;
import com.tulumcore.api.entities.Usuario;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.services.CategoriaService;
import com.tulumcore.api.services.ProductoService;
import com.tulumcore.api.services.RecetaService;
import com.tulumcore.api.services.StockMovementService;
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

    @Autowired
    private StockMovementService stockMovementService;

    @Autowired
    private RecetaService recetaService;

    @GetMapping
    public List<ProductoResponseDTO> getAllProductos() {
        return productoService.getAllProductos()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @GetMapping("/buscar")
    public List<ProductoResponseDTO> buscarProductos(@RequestParam("q") String query) {
        return productoService.buscarPorNombre(query)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @GetMapping("/codigo/{codigoBarras}")
    public ProductoResponseDTO getProductoPorCodigoBarras(@PathVariable String codigoBarras) {
        return productoService.buscarPorCodigoBarras(codigoBarras)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado para el codigo de barras indicado."));
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
        producto.setPrecioCosto(dto.getPrecioCosto());
        producto.setMargenPorcentaje(dto.getMargenPorcentaje());
        double stockInicial = dto.getCantidadStock() != null ? dto.getCantidadStock() : 0;
        producto.setCantidadStock(0d);
        producto.setStockMinimo(dto.getStockMinimo());
        producto.setMedidas(dto.getMedidas());
        producto.setCodigoBarras(dto.getCodigoBarras());
        producto.setImageUrl(dto.getImageUrl());
        producto.setCategoria(categoria);
        producto.setTipo(dto.getTipo());
        if (dto.getVendible() != null) {
            producto.setVendible(dto.getVendible());
        } else {
            producto.setVendible(!ProductoTipo.esInsumo(producto.getTipo()));
        }
        if (dto.getPublicadoEnCatalogo() != null) {
            producto.setPublicadoEnCatalogo(dto.getPublicadoEnCatalogo());
        } else {
            producto.setPublicadoEnCatalogo(productoService.catalogoPublicoHabilitado());
        }

        Producto saved = productoService.createOrUpdateProducto(producto);
        if (ProductoTipo.esInsumo(saved.getTipo())) {
            recetaService.guardar(saved, java.util.List.of());
        } else {
            recetaService.guardar(saved, dto.getReceta());
        }

        if (stockInicial > 0) {
            Usuario usuario = stockMovementService.getCurrentUser();
            stockMovementService.registrar(MovementType.AJUSTE, saved, usuario,
                    stockInicial, "Stock inicial al crear producto", null, null);
        }

        return ResponseEntity.ok(toDTO(saved));
    }

    @PutMapping("/{id}")
    public ProductoResponseDTO updateProducto(@PathVariable Long id, @RequestBody ProductoDTO dto) {
        Producto existente = productoService.getProductoById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));

        existente.setNombre(dto.getNombre());
        existente.setDescripcion(dto.getDescripcion());
        existente.setPrecio(dto.getPrecio());
        existente.setPrecioCosto(dto.getPrecioCosto());
        existente.setMargenPorcentaje(dto.getMargenPorcentaje());
        if (dto.getCantidadStock() != null) {
            existente.setCantidadStock(dto.getCantidadStock());
        }
        existente.setStockMinimo(dto.getStockMinimo());
        existente.setMedidas(dto.getMedidas());
        existente.setCodigoBarras(dto.getCodigoBarras());

        if (dto.getImageUrl() != null) {
            existente.setImageUrl(dto.getImageUrl());
        }
        if (dto.getPublicadoEnCatalogo() != null) {
            existente.setPublicadoEnCatalogo(dto.getPublicadoEnCatalogo());
        }

        if (dto.getCategoriaId() != null) {
            Categoria categoria = categoriaService.getCategoriaById(dto.getCategoriaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + dto.getCategoriaId()));
            existente.setCategoria(categoria);
        }
        if (dto.getTipo() != null) {
            existente.setTipo(dto.getTipo());
        }
        if (dto.getVendible() != null) {
            existente.setVendible(dto.getVendible());
        }

        Producto saved = productoService.createOrUpdateProducto(existente);
        if (ProductoTipo.esInsumo(saved.getTipo())) {
            recetaService.guardar(saved, java.util.List.of());
        } else if (dto.getReceta() != null) {
            recetaService.guardar(saved, dto.getReceta());
        }
        return toDTO(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProducto(@PathVariable Long id) {
        productoService.getProductoById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        productoService.deleteProducto(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/catalogo")
    public ProductoResponseDTO publicarEnCatalogo(
            @PathVariable Long id,
            @RequestBody CatalogoProductoDTO dto
    ) {
        if (dto == null || dto.publicadoEnCatalogo() == null) {
            throw new BusinessException("Indicá si el producto se publica en la tienda.");
        }
        Producto existente = productoService.getProductoById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        existente.setPublicadoEnCatalogo(dto.publicadoEnCatalogo());
        return toDTO(productoService.createOrUpdateProducto(existente));
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
            String unidad = p.getCategoria().getUnidadMedida();
            categoriaDTO.setUnidadMedida(unidad != null && !unidad.isBlank() ? unidad : "UNIDAD");
        }

        return new ProductoResponseDTO(
                p.getId(),
                p.getNombre(),
                p.getDescripcion(),
                p.getPrecio(),
                p.getPrecioCosto(),
                p.getMargenPorcentaje(),
                p.getCantidadStock(),
                p.getStockMinimo(),
                p.getMedidas(),
                p.getCodigoBarras(),
                p.getImageUrl(),
                p.isPublicadoEnCatalogo(),
                p.getTipo(),
                p.isVendible(),
                recetaService.porcionesEstimadas(p),
                recetaService.listar(p.getId()),
                categoriaDTO
        );
    }
}

package com.tulumcore.api.controllers;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.Producto;
import com.tulumcore.api.repositories.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alertas")
public class AlertaStockController {

    @Autowired
    private ProductoRepository productoRepository;

    /**
     * Devuelve productos cuyo stock actual es menor o igual al stock mínimo configurado.
     * Stock mínimo = 0 significa sin alerta configurada, se ignora.
     */
    @GetMapping("/stock-minimo")
    public List<ProductoResponseDTO> getProductosBajoStock() {
        String tenant = TenantContext.getCurrentTenant();
        return productoRepository.findAllByTenantId(tenant)
                .stream()
                .filter(p -> p.getStockMinimo() != null
                        && p.getStockMinimo() > 0
                        && p.getCantidadStock() <= p.getStockMinimo())
                .map(p -> {
                    CategoriaDTO cat = null;
                    if (p.getCategoria() != null) {
                        cat = new CategoriaDTO();
                        cat.setId(p.getCategoria().getId());
                        cat.setNombre(p.getCategoria().getNombre());
                        String unidad = p.getCategoria().getUnidadMedida();
                        cat.setUnidadMedida(unidad != null && !unidad.isBlank() ? unidad : "UNIDAD");
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
                            p.getCantidadStock() != null ? p.getCantidadStock() : 0,
                            java.util.List.of(),
                            cat
                    );
                })
                .toList();
    }
}

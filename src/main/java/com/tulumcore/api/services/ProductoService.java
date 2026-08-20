package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.FeatureKey;
import com.tulumcore.api.entities.MovementType;
import com.tulumcore.api.entities.Producto;
import com.tulumcore.api.entities.TenantConfig;
import com.tulumcore.api.entities.Usuario;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.repositories.ProductoRepository;
import com.tulumcore.api.repositories.TenantConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private StockMovementService stockMovementService;

    @Autowired
    private AuditoryLogService auditoryLogService;

    @Autowired
    private TenantFeatureService tenantFeatureService;

    @Autowired
    private TenantConfigRepository tenantConfigRepository;

    /**
     * Margen configurado para el tenant vigente. null = el tenant carga el precio
     * de venta a mano y no queremos derivarlo del costo.
     */
    public Double getMargenPorDefecto() {
        String tenant = TenantContext.getCurrentTenant();
        return tenantConfigRepository.findByTenantId(tenant)
                .map(TenantConfig::getMargenPorDefecto)
                .orElse(null);
    }

    /**
     * Precio de venta derivado del costo. Devuelve null si falta el costo o si no hay
     * margen aplicable, para que el llamador respete el precio cargado a mano.
     */
    public Double calcularPrecioVenta(Double precioCosto, Double margenProducto) {
        if (precioCosto == null) {
            return null;
        }
        Double margen = margenProducto != null ? margenProducto : getMargenPorDefecto();
        if (margen == null) {
            return null;
        }
        return precioCosto * (1 + margen / 100.0);
    }

    public List<Producto> getAllProductos() {
        String tenant = TenantContext.getCurrentTenant();
        return productoRepository.findAllByTenantId(tenant);
    }

    public Optional<Producto> getProductoById(Long id) {
        String tenant = TenantContext.getCurrentTenant();
        return productoRepository.findByIdAndTenantId(id, tenant);
    }

    public List<Producto> buscarPorNombre(String query) {
        String tenant = TenantContext.getCurrentTenant();
        String normalizedQuery = query != null ? query.trim() : "";
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }
        return productoRepository.buscarCatalogo(tenant, normalizedQuery, PageRequest.of(0, 40));
    }

    public Optional<Producto> buscarPorCodigoBarras(String codigoBarras) {
        tenantFeatureService.requireEnabled(FeatureKey.POS_BARCODE);
        String tenant = TenantContext.getCurrentTenant();
        String normalizedCodigo = normalizarCodigoBarras(codigoBarras);
        if (normalizedCodigo == null) {
            return Optional.empty();
        }
        return productoRepository.findByCodigoBarrasAndTenantId(normalizedCodigo, tenant);
    }

    public Producto createOrUpdateProducto(Producto producto) {
        boolean isNew = producto.getId() == null;
        String tenant = TenantContext.getCurrentTenant();
        producto.setTenantId(tenant);
        producto.setCodigoBarras(normalizarCodigoBarras(producto.getCodigoBarras()));
        validarCodigoBarrasUnico(producto, tenant);

        // Si sólo llegó el costo, derivamos el precio de venta con el margen del tenant.
        if (producto.getPrecio() == null) {
            Double precioDerivado = calcularPrecioVenta(producto.getPrecioCosto(), producto.getMargenPorcentaje());
            if (precioDerivado == null) {
                throw new BusinessException("Cargá el precio de venta o el precio de costo con un margen configurado.");
            }
            producto.setPrecio(precioDerivado);
        }

        String detalleAnterior = null;
        if (!isNew) {
            detalleAnterior = productoRepository.findByIdAndTenantId(producto.getId(), tenant)
                    .map(this::detalleProducto)
                    .orElse(null);
        }

        Producto saved = productoRepository.save(producto);
        auditoryLogService.registrar(isNew ? "CREATE" : "UPDATE", "PRODUCTO", saved.getId(),
                (isNew ? "Producto creado: " : "Producto actualizado: ") + saved.getNombre(),
                detalleAnterior, detalleProducto(saved));
        return saved;
    }

    public void deleteProducto(Long id) {
        getProductoById(id).ifPresent(p -> {
            String detalleAnterior = detalleProducto(p);
            productoRepository.delete(p);
            auditoryLogService.registrar("DELETE", "PRODUCTO", id,
                    "Producto eliminado: " + p.getNombre(), detalleAnterior, null);
        });
    }

    @Transactional
    public void adjustStock(Long id, double cantidad) {
        Producto producto = getProductoById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        Usuario usuario = stockMovementService.getCurrentUser();
        stockMovementService.registrar(MovementType.AJUSTE, producto, usuario,
                cantidad, "Ajuste manual de stock", null, null);
    }

    public List<Producto> getLatestProductos(int limit) {
        String tenant = TenantContext.getCurrentTenant();
        return productoRepository.findAllByTenantId(tenant)
                .stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .limit(limit)
                .toList();
    }

    private String detalleProducto(Producto producto) {
        return auditoryLogService.detalle(
                "nombre", producto.getNombre(),
                "precio", producto.getPrecio(),
                "precioCosto", producto.getPrecioCosto(),
                "margenPorcentaje", producto.getMargenPorcentaje(),
                "stock", producto.getCantidadStock(),
                "stockMinimo", producto.getStockMinimo(),
                "medidas", producto.getMedidas(),
                "codigoBarras", producto.getCodigoBarras(),
                "categoriaId", producto.getCategoria() != null ? producto.getCategoria().getId() : null
        );
    }

    private String normalizarCodigoBarras(String codigoBarras) {
        if (codigoBarras == null || codigoBarras.trim().isEmpty()) {
            return null;
        }
        return codigoBarras.trim();
    }

    private void validarCodigoBarrasUnico(Producto producto, String tenant) {
        String codigoBarras = producto.getCodigoBarras();
        if (codigoBarras == null) {
            return;
        }

        productoRepository.findByCodigoBarrasAndTenantId(codigoBarras, tenant)
                .filter(existing -> producto.getId() == null || !existing.getId().equals(producto.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException("Ya existe un producto con este codigo de barras.");
                });
    }
}

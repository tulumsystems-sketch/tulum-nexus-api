package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.MovementType;
import com.tulumcore.api.entities.Producto;
import com.tulumcore.api.entities.Usuario;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.repositories.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
        return productoRepository.findByNombreContainingIgnoreCaseAndTenantId(query, tenant);
    }

    public Producto createOrUpdateProducto(Producto producto) {
        boolean isNew = producto.getId() == null;
        String tenant = TenantContext.getCurrentTenant();
        producto.setTenantId(tenant);

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
    public void adjustStock(Long id, int cantidad) {
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
                "stock", producto.getCantidadStock(),
                "stockMinimo", producto.getStockMinimo(),
                "medidas", producto.getMedidas(),
                "categoriaId", producto.getCategoria() != null ? producto.getCategoria().getId() : null
        );
    }
}

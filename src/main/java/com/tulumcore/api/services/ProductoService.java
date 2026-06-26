package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.AuditoryLog;
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
        Producto saved = productoRepository.save(producto);
        if (isNew) {
            auditoryLogService.registrar("CREATE", "PRODUCTO", saved.getId(),
                    "Se creó el producto: " + saved.getNombre(), null, null);
        } else {
            auditoryLogService.registrar("UPDATE", "PRODUCTO", saved.getId(),
                    "Se actualizó el producto: " + saved.getNombre(), null, null);
        }
        return saved;
    }

    public void deleteProducto(Long id) {
        getProductoById(id).ifPresent(p -> {
            productoRepository.deleteById(id);
            auditoryLogService.registrar("DELETE", "PRODUCTO", id,
                    "Se eliminó el producto: " + p.getNombre(), null, null);
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
}
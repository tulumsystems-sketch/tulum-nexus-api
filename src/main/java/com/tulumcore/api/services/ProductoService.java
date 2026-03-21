package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.Producto;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.repositories.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

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
        return productoRepository.save(producto);
    }

    public void deleteProducto(Long id) {
        getProductoById(id).ifPresent(p -> productoRepository.deleteById(id));
    }

    public void adjustStock(Long id, int cantidad) {
        Producto producto = getProductoById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        producto.setCantidadStock(producto.getCantidadStock() + cantidad);
        productoRepository.save(producto);
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
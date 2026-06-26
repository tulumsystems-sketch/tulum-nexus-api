package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.*;
import com.tulumcore.api.repositories.StockMovementRepository;
import com.tulumcore.api.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockMovementService {

    @Autowired
    private StockMovementRepository repository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public List<StockMovement> listar() {
        return repository.findAllByTenantIdOrderByFechaDesc(TenantContext.getCurrentTenant());
    }

    public List<StockMovement> listarPorProducto(Long productoId) {
        return repository.findAllByTenantIdAndProductoIdOrderByFechaDesc(
                TenantContext.getCurrentTenant(), productoId);
    }

    public List<StockMovement> buscarPorFiltros(MovementType tipo, LocalDateTime desde, LocalDateTime hasta) {
        return repository.buscarPorFiltros(TenantContext.getCurrentTenant(), tipo, desde, hasta);
    }

    public Usuario getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String tenant = TenantContext.getCurrentTenant();
        return usuarioRepository.findByEmailAndTenantId(email, tenant)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));
    }

    @Transactional
    public StockMovement registrar(
            MovementType tipo, Producto producto, Usuario usuario,
            Integer cantidad, String motivo, Venta venta, Compra compra) {

        StockMovement mov = new StockMovement();
        mov.setTipoMovimiento(tipo);
        mov.setProducto(producto);
        mov.setUsuario(usuario);
        mov.setCantidad(cantidad);
        mov.setMotivo(motivo);
        mov.setVenta(venta);
        mov.setCompra(compra);
        mov.setTenantId(TenantContext.getCurrentTenant());

        if (tipo == MovementType.COMPRA || tipo == MovementType.AJUSTE) {
            producto.setCantidadStock(producto.getCantidadStock() + cantidad);
        } else {
            producto.setCantidadStock(producto.getCantidadStock() - cantidad);
        }

        return repository.save(mov);
    }
}

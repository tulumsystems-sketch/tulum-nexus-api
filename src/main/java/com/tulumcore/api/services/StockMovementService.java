package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.*;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.repositories.ProductoRepository;
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

    private static final double TOLERANCIA = 0.0001;

    @Autowired
    private StockMovementRepository repository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProductoRepository productoRepository;

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
            Number cantidad, String motivo, Venta venta, Compra compra) {
        return registrar(tipo, producto, usuario, cantidad, motivo, venta, compra, null);
    }

    @Transactional
    public StockMovement registrar(
            MovementType tipo, Producto producto, Usuario usuario,
            Number cantidad, String motivo, Venta venta, Compra compra, Remito remito) {

        double qty = cantidad != null ? cantidad.doubleValue() : 0;
        validarMovimiento(tipo, producto, qty);

        StockMovement mov = new StockMovement();
        mov.setTipoMovimiento(tipo);
        mov.setProducto(producto);
        mov.setUsuario(usuario);
        mov.setCantidad(qty);
        mov.setMotivo(motivo);
        mov.setVenta(venta);
        mov.setCompra(compra);
        mov.setRemito(remito);
        mov.setTenantId(TenantContext.getCurrentTenant());

        producto.setCantidadStock(calcularStockResultante(tipo, producto, qty));
        Producto productoActualizado = productoRepository.save(producto);
        mov.setProducto(productoActualizado);

        return repository.save(mov);
    }

    private void validarMovimiento(MovementType tipo, Producto producto, double cantidad) {
        if (producto == null) {
            throw new BusinessException("Producto requerido para registrar movimiento de stock.");
        }
        if (Math.abs(cantidad) < TOLERANCIA) {
            throw new BusinessException("La cantidad del movimiento de stock debe ser distinta de cero.");
        }
        if (tipo != MovementType.AJUSTE && cantidad < 0) {
            throw new BusinessException("La cantidad debe ser positiva para movimientos de tipo " + tipo + ".");
        }

        double stockResultante = calcularStockResultante(tipo, producto, cantidad);
        if (stockResultante < -TOLERANCIA) {
            throw new BusinessException("Stock insuficiente para " + producto.getNombre()
                    + ". Disponible: " + stockActual(producto) + ", requerido: " + cantidad + ".");
        }
    }

    private double calcularStockResultante(MovementType tipo, Producto producto, double cantidad) {
        double stock = stockActual(producto);
        if (tipo == MovementType.COMPRA || tipo == MovementType.AJUSTE) {
            return stock + cantidad;
        }
        return stock - cantidad;
    }

    private double stockActual(Producto producto) {
        return producto.getCantidadStock() != null ? producto.getCantidadStock() : 0;
    }
}

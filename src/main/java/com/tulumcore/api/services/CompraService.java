package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.controllers.CompraDTO;
import com.tulumcore.api.controllers.ItemCompraDTO;
import com.tulumcore.api.entities.*;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.repositories.CompraRepository;
import com.tulumcore.api.repositories.ProductoRepository;
import com.tulumcore.api.repositories.ProveedorRepository;
import com.tulumcore.api.repositories.RemitoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CompraService {

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private StockMovementService stockMovementService;

    @Autowired
    private AuditoryLogService auditoryLogService;

    @Autowired
    private RemitoRepository remitoRepository;

    public List<Compra> getAll() {
        return compraRepository.findAllByTenantIdOrderByFechaDesc(TenantContext.getCurrentTenant());
    }

    public List<Map<String, Object>> obtenerSugerenciasCompra() {
        String tenant = TenantContext.getCurrentTenant();
        
        // 1. Obtener todos los productos del tenant
        List<Producto> productos = productoRepository.findAllByTenantId(tenant);
        
        // 2. Obtener remitos pendientes o en viaje (demanda comprometida)
        List<Remito> remitosPendientes = remitoRepository.findByTenantIdAndEstadoIn(tenant, List.of("PENDIENTE", "EN_VIAJE"));
        
        // 3. Calcular demanda por producto
        Map<Long, Integer> demandaPorProducto = new HashMap<>();
        for (Remito r : remitosPendientes) {
            if (r.getItems() != null) {
                for (ItemRemito item : r.getItems()) {
                    if (item.getProducto() != null) {
                        demandaPorProducto.merge(item.getProducto().getId(), item.getCantidad() != null ? item.getCantidad() : 0, Integer::sum);
                    }
                }
            }
        }
        
        // 4. Calcular faltantes comparando stock actual, demanda y stock mínimo
        List<Map<String, Object>> sugerencias = new ArrayList<>();
        for (Producto p : productos) {
            double stockActual = p.getCantidadStock() != null ? p.getCantidadStock() : 0;
            int stockMinimo = p.getStockMinimo() != null ? p.getStockMinimo() : 0;
            int demandaRemitos = demandaPorProducto.getOrDefault(p.getId(), 0);
            
            // Formula: Faltante = Demanda de Remitos + Stock Mínimo - Stock Actual
            int sugerido = (int) Math.ceil(demandaRemitos + stockMinimo - stockActual);
            
            if (sugerido > 0) {
                Map<String, Object> sug = new HashMap<>();
                sug.put("productoId", p.getId());
                sug.put("nombreProducto", p.getNombre());
                sug.put("stockActual", stockActual);
                sug.put("stockMinimo", stockMinimo);
                sug.put("demandaRemitos", demandaRemitos);
                sug.put("cantidadSugerida", sugerido);
                sug.put("precioEstimado", p.getPrecio() != null ? p.getPrecio() : 0.0);
                sugerencias.add(sug);
            }
        }
        
        return sugerencias;
    }

    @Transactional
    public Compra crear(CompraDTO dto) {
        String tenant = TenantContext.getCurrentTenant();

        Proveedor proveedor = proveedorRepository.findByIdAndTenantId(dto.getProveedorId(), tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));

        Compra compra = new Compra();
        compra.setTenantId(tenant);
        compra.setFecha(LocalDateTime.now());
        compra.setProveedor(proveedor);
        compra.setNroFactura(dto.getNroFactura());
        compra.setObservaciones(dto.getObservaciones());
        compra.setEstado("PENDIENTE");

        List<ItemCompra> items = new ArrayList<>();
        double total = 0;

        for (ItemCompraDTO itemDto : dto.getItems()) {
            Producto producto = productoRepository.findByIdAndTenantId(itemDto.getProductoId(), tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + itemDto.getProductoId()));

            ItemCompra item = new ItemCompra();
            item.setCompra(compra);
            item.setProducto(producto);
            item.setCantidad(itemDto.getCantidad());
            item.setPrecioUnitario(itemDto.getPrecioUnitario());
            item.setTenantId(tenant);
            items.add(item);
            total += itemDto.getPrecioUnitario() * itemDto.getCantidad();
        }

        compra.setItems(items);
        compra.setTotal(total);

        Compra saved = compraRepository.save(compra);

        auditoryLogService.registrar("CREATE", "COMPRA", saved.getId(),
                "Orden de compra creada - Proveedor: " + proveedor.getNombre() +
                " - $" + String.format("%.2f", total),
                null, detalleCompra(saved));

        return saved;
    }

    @Transactional
    public Compra recibirMercaderia(Long id) {
        String tenant = TenantContext.getCurrentTenant();

        Compra compra = compraRepository.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada con id: " + id));

        if (!"PENDIENTE".equals(compra.getEstado())) {
            throw new BusinessException("La orden ya fue recibida o procesada.");
        }

        Usuario usuario = stockMovementService.getCurrentUser();
        String detalleAnterior = detalleCompra(compra);

        for (ItemCompra item : compra.getItems()) {
            stockMovementService.registrar(
                    MovementType.COMPRA,
                    item.getProducto(),
                    usuario,
                    item.getCantidad(),
                    "Recepción de Orden de Compra #" + compra.getId(),
                    null,
                    compra
            );
        }

        compra.setEstado("RECIBIDA");
        Compra saved = compraRepository.save(compra);

        auditoryLogService.registrar("UPDATE", "COMPRA", saved.getId(),
                "Orden de compra recibida - Proveedor: " +
                (compra.getProveedor() != null ? compra.getProveedor().getNombre() : "N/A"),
                detalleAnterior, detalleCompra(saved));

        return saved;
    }

    public void delete(Long id) {
        String tenant = TenantContext.getCurrentTenant();
        Compra compra = compraRepository.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada con id: " + id));
        String detalleAnterior = detalleCompra(compra);
        compraRepository.delete(compra);
        auditoryLogService.registrar("DELETE", "COMPRA", id,
                "Orden de compra eliminada", detalleAnterior, null);
    }

    private String detalleCompra(Compra compra) {
        return auditoryLogService.detalle(
                "estado", compra.getEstado(),
                "proveedor", compra.getProveedor() != null ? compra.getProveedor().getNombre() : null,
                "nroFactura", compra.getNroFactura(),
                "total", compra.getTotal(),
                "items", compra.getItems() != null ? compra.getItems().size() : 0
        );
    }
}

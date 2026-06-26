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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    public List<Compra> getAll() {
        return compraRepository.findAllByTenantIdOrderByFechaDesc(TenantContext.getCurrentTenant());
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
                null, null);

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
                null, null);

        return saved;
    }

    public void delete(Long id) {
        String tenant = TenantContext.getCurrentTenant();
        Compra compra = compraRepository.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada con id: " + id));
        compraRepository.delete(compra);
    }
}

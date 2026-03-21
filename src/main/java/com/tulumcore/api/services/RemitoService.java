package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.controllers.ItemRemitoDTO;
import com.tulumcore.api.controllers.RemitoDTO;
import com.tulumcore.api.entities.*;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class RemitoService {

    @Autowired private RemitoRepository remitoRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProductoRepository productoRepository;

    public List<Remito> getAll() {
        String tenant = TenantContext.getCurrentTenant();
        return remitoRepository.findAllByTenantIdOrderByFechaDesc(tenant);
    }

    public List<Remito> getByEstado(String estado) {
        String tenant = TenantContext.getCurrentTenant();
        return remitoRepository.findAllByTenantIdAndEstadoOrderByFechaDesc(tenant, estado);
    }

    @Transactional
    public Remito crear(RemitoDTO dto) {
        String tenant = TenantContext.getCurrentTenant();

        Remito remito = new Remito();
        remito.setTenantId(tenant);
        remito.setFecha(LocalDateTime.now());
        remito.setNroRemito(generarNroRemito());
        remito.setDireccionEntrega(dto.getDireccionEntrega());
        remito.setNombreDestinatario(dto.getNombreDestinatario());
        remito.setTelefonoDestinatario(dto.getTelefonoDestinatario());
        remito.setObservaciones(dto.getObservaciones());
        remito.setEstado("PENDIENTE");

        if (dto.getClienteId() != null) {
            Cliente cliente = clienteRepository.findByIdAndTenantId(dto.getClienteId(), tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
            remito.setCliente(cliente);
        }

        List<ItemRemito> items = new ArrayList<>();
        for (ItemRemitoDTO itemDto : dto.getItems()) {
            ItemRemito item = new ItemRemito();
            item.setRemito(remito);
            item.setCantidad(itemDto.getCantidad());
            item.setDescripcion(itemDto.getDescripcion());
            item.setTenantId(tenant);

            if (itemDto.getProductoId() != null) {
                Producto producto = productoRepository.findByIdAndTenantId(itemDto.getProductoId(), tenant)
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
                item.setProducto(producto);
                if (item.getDescripcion() == null) {
                    item.setDescripcion(producto.getNombre());
                }
            }
            items.add(item);
        }

        remito.setItems(items);
        return remitoRepository.save(remito);
    }

    @Transactional
    public Remito cambiarEstado(Long id, String nuevoEstado) {
        String tenant = TenantContext.getCurrentTenant();

        List<String> estadosValidos = List.of("PENDIENTE", "EN_VIAJE", "ENTREGADO", "INCIDENCIA");
        if (!estadosValidos.contains(nuevoEstado)) {
            throw new BusinessException("Estado inválido: " + nuevoEstado);
        }

        Remito remito = remitoRepository.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Remito no encontrado con id: " + id));

        if ("ENTREGADO".equals(remito.getEstado())) {
            throw new BusinessException("Un remito entregado no puede cambiar de estado.");
        }

        remito.setEstado(nuevoEstado);
        return remitoRepository.save(remito);
    }

    private String generarNroRemito() {
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = remitoRepository.count() + 1;
        return "R-" + fecha + "-" + String.format("%04d", count);
    }
}
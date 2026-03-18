package com.tulumcore.api.services;

import com.tulumcore.api.controllers.VentaResumenDTO;
import com.tulumcore.api.controllers.VentaDTO;
import com.tulumcore.api.controllers.ItemVentaDTO;
import com.tulumcore.api.entities.*;
import com.tulumcore.api.repositories.*;
import com.tulumcore.api.config.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VentaService {

    @Autowired private VentaRepository ventaRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private CajaRepository cajaRepository;

    @Transactional
    public Venta guardar(VentaDTO dto) {
        String tenant = TenantContext.getCurrentTenant();
        Caja caja = cajaRepository.findByEstadoAndTenantId("ABIERTA", tenant)
                .orElseThrow(() -> new RuntimeException("Debe abrir caja para realizar ventas."));

        Venta venta = new Venta();
        venta.setObservaciones(dto.getObservaciones());
        venta.setMoneda("ARS");
        venta.setMetodoPago(dto.getMetodoPago() != null ? dto.getMetodoPago() : "MERCADO_PAGO");
        venta.setTenantId(tenant);

        if (dto.getClienteId() != null && dto.getClienteId() > 0) {
            venta.setCliente(clienteRepository.findById(dto.getClienteId()).orElse(null));
        }

        List<ItemVenta> items = new ArrayList<>();
        double subtotal = 0;

        for (ItemVentaDTO itemDto : dto.getItems()) {
            Producto p = productoRepository.findById(itemDto.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            if (p.getCantidadStock() < itemDto.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para: " + p.getNombre());
            }

            p.setCantidadStock(p.getCantidadStock() - itemDto.getCantidad());
            productoRepository.save(p);

            ItemVenta item = new ItemVenta();
            item.setVenta(venta);
            item.setProducto(p);
            item.setCantidad(itemDto.getCantidad());
            item.setPrecioUnitario(p.getPrecio());
            items.add(item);
            subtotal += p.getPrecio() * itemDto.getCantidad();
        }

        venta.setItems(items);
        venta.setTotalNeto(subtotal);
        venta.setTotalIva(subtotal * 0.21);
        double totalFinal = subtotal * 1.21;
        venta.setTotalFinal(totalFinal);

        if ("EFECTIVO".equalsIgnoreCase(venta.getMetodoPago())) {
            venta.setEstado("PAGADA");
            double abonado = dto.getMontoAbonado() != null ? dto.getMontoAbonado() : totalFinal;
            venta.setMontoAbonado(abonado);
            venta.setVuelto(Math.max(0, abonado - totalFinal));
            caja.setMontoVentasEfectivo(caja.getMontoVentasEfectivo() + totalFinal);
        } else {
            venta.setEstado("PAGADA");
            caja.setMontoVentasMP(caja.getMontoVentasMP() + totalFinal);
        }

        caja.setMontoFinalEsperado(caja.getMontoInicial() + caja.getMontoVentasEfectivo());
        cajaRepository.save(caja);
        return ventaRepository.save(venta);
    }

    public Page<Venta> buscarVentas(String tenantId, LocalDate desde, LocalDate hasta, String metodoPago, String estado, Pageable pageable) {
        return ventaRepository.findAll((Specification<Venta>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));

            if (desde != null) predicates.add(cb.greaterThanOrEqualTo(root.get("fecha"), desde.atStartOfDay()));
            if (hasta != null) predicates.add(cb.lessThanOrEqualTo(root.get("fecha"), hasta.atTime(23, 59, 59)));
            if (metodoPago != null) predicates.add(cb.equal(root.get("metodoPago"), metodoPago));
            if (estado != null) predicates.add(cb.equal(root.get("estado"), estado));

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    public List<VentaResumenDTO> obtenerResumenSemanal(String tenantId) {
        LocalDateTime haceSieteDias = LocalDateTime.now().minusDays(7);
        List<Venta> ventas = ventaRepository.findByTenantIdAndFechaAfter(tenantId, haceSieteDias);

        Map<LocalDate, List<Venta>> agrupadas = ventas.stream()
                .filter(v -> !"ANULADA".equals(v.getEstado()))
                .collect(Collectors.groupingBy(v -> v.getFecha().toLocalDate()));

        Map<LocalDate, VentaResumenDTO> resumenMap = new TreeMap<>();
        agrupadas.forEach((fecha, lista) -> {
            double ef = lista.stream().filter(v -> "EFECTIVO".equalsIgnoreCase(v.getMetodoPago())).mapToDouble(Venta::getTotalFinal).sum();
            double mp = lista.stream().filter(v -> "MERCADO_PAGO".equalsIgnoreCase(v.getMetodoPago())).mapToDouble(Venta::getTotalFinal).sum();
            resumenMap.put(fecha, new VentaResumenDTO(fecha, ef, mp));
        });
        return new ArrayList<>(resumenMap.values());
    }
}
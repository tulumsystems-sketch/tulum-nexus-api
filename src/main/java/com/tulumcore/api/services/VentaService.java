package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.controllers.ItemVentaDTO;
import com.tulumcore.api.controllers.VentaDTO;
import com.tulumcore.api.controllers.VentaResumenDTO;
import com.tulumcore.api.entities.*;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.repositories.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Autowired private StockMovementService stockMovementService;
    @Autowired private AuditoryLogService auditoryLogService;

    @Transactional
    public Venta guardar(VentaDTO dto) {
        String tenant = TenantContext.getCurrentTenant();
        Caja caja = cajaRepository.findByEstadoAndTenantId("ABIERTA", tenant)
                .orElseThrow(() -> new BusinessException("Debe abrir caja para realizar ventas."));

        Venta venta = new Venta();
        venta.setObservaciones(dto.getObservaciones());
        venta.setMoneda("ARS");
        venta.setMetodoPago(dto.getMetodoPago() != null ? dto.getMetodoPago() : "MERCADO_PAGO");
        venta.setTenantId(tenant);

        if (dto.getClienteId() != null && dto.getClienteId() > 0) {
            venta.setCliente(clienteRepository.findByIdAndTenantId(dto.getClienteId(), tenant).orElse(null));
        }

        List<ItemVenta> items = new ArrayList<>();
        Map<Long, Integer> cantidadesPorProducto = new HashMap<>();
        double subtotal = 0;

        Usuario usuario = stockMovementService.getCurrentUser();

        for (ItemVentaDTO itemDto : dto.getItems()) {
            Producto p = productoRepository.findByIdAndTenantId(itemDto.getProductoId(), tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + itemDto.getProductoId()));

            int cantidad = itemDto.getCantidad() != null ? itemDto.getCantidad() : 0;
            if (cantidad <= 0) {
                throw new BusinessException("La cantidad vendida debe ser mayor a cero para: " + p.getNombre());
            }

            int cantidadAcumulada = cantidadesPorProducto.merge(p.getId(), cantidad, Integer::sum);
            int stockDisponible = p.getCantidadStock() != null ? p.getCantidadStock() : 0;
            if (stockDisponible < cantidadAcumulada) {
                throw new BusinessException("Stock insuficiente para: " + p.getNombre()
                        + ". Disponible: " + stockDisponible + ", requerido: " + cantidadAcumulada + ".");
            }

            ItemVenta item = new ItemVenta();
            item.setVenta(venta);
            item.setProducto(p);
            item.setCantidad(cantidad);
            item.setPrecioUnitario(p.getPrecio());
            item.setTenantId(tenant);
            items.add(item);
            subtotal += p.getPrecio() * cantidad;
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

        Venta saved = ventaRepository.save(venta);

        for (ItemVenta item : saved.getItems()) {
            stockMovementService.registrar(MovementType.VENTA, item.getProducto(), usuario,
                    item.getCantidad(), "Venta #" + saved.getId(), saved, null);
        }

        String clienteNombre = saved.getCliente() != null
                ? saved.getCliente().getNombre() + " " + saved.getCliente().getApellido()
                : "Consumidor Final";
        auditoryLogService.registrar("CREATE", "VENTA", saved.getId(),
                "Venta #" + saved.getId() + " - " + clienteNombre + " - $" +
                        String.format("%.2f", saved.getTotalFinal()) + " (" + saved.getMetodoPago() + ")",
                null, detalleVenta(saved));

        return saved;
    }

    @Transactional
    public Venta anularVenta(Long id) {
        String tenant = TenantContext.getCurrentTenant();

        Venta venta = ventaRepository.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con id: " + id));

        if ("ANULADA".equals(venta.getEstado())) {
            throw new BusinessException("La venta ya fue anulada anteriormente.");
        }

        Usuario usuario = stockMovementService.getCurrentUser();
        String detalleAnterior = detalleVenta(venta);

        for (ItemVenta item : venta.getItems()) {
            stockMovementService.registrar(MovementType.AJUSTE, item.getProducto(), usuario,
                    item.getCantidad(), "Devolucion por anulacion de venta #" + venta.getId(), venta, null);
        }

        cajaRepository.findByEstadoAndTenantId("ABIERTA", tenant).ifPresent(caja -> {
            if ("EFECTIVO".equalsIgnoreCase(venta.getMetodoPago())) {
                caja.setMontoVentasEfectivo(Math.max(0, caja.getMontoVentasEfectivo() - venta.getTotalFinal()));
                caja.setMontoFinalEsperado(caja.getMontoInicial() + caja.getMontoVentasEfectivo());
            } else {
                caja.setMontoVentasMP(Math.max(0, caja.getMontoVentasMP() - venta.getTotalFinal()));
            }
            cajaRepository.save(caja);
        });

        venta.setEstado("ANULADA");
        Venta saved = ventaRepository.save(venta);
        String clienteNombre = saved.getCliente() != null
                ? saved.getCliente().getNombre() + " " + saved.getCliente().getApellido()
                : "Consumidor Final";
        auditoryLogService.registrar("UPDATE", "VENTA", saved.getId(),
                "Venta #" + saved.getId() + " anulada - " + clienteNombre, detalleAnterior, detalleVenta(saved));
        return saved;
    }

    public Page<Venta> buscarVentas(String tenantId, LocalDate desde, LocalDate hasta,
                                    String metodoPago, String estado, Pageable pageable) {
        return ventaRepository.findAll((Specification<Venta>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (desde != null) predicates.add(cb.greaterThanOrEqualTo(root.get("fecha"), desde.atStartOfDay()));
            if (hasta != null) predicates.add(cb.lessThanOrEqualTo(root.get("fecha"), hasta.atTime(23, 59, 59)));
            if (metodoPago != null && !metodoPago.isEmpty()) predicates.add(cb.equal(root.get("metodoPago"), metodoPago));
            if (estado != null && !estado.isEmpty()) predicates.add(cb.equal(root.get("estado"), estado));
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    public List<Venta> getAllVentas(String tenantId) {
        return ventaRepository.findByTenantId(tenantId);
    }

    public VentaResumenDTO obtenerResumenHoy(String tenantId) {
        LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
        List<Venta> ventas = ventaRepository.findByTenantIdAndFechaAfter(tenantId, inicioHoy);

        List<Venta> validas = ventas.stream()
                .filter(v -> !"ANULADA".equals(v.getEstado()))
                .toList();

        double ef = validas.stream().filter(v -> "EFECTIVO".equalsIgnoreCase(v.getMetodoPago()))
                .mapToDouble(Venta::getTotalFinal).sum();
        double mp = validas.stream().filter(v -> "MERCADO_PAGO".equalsIgnoreCase(v.getMetodoPago()))
                .mapToDouble(Venta::getTotalFinal).sum();

        return new VentaResumenDTO(LocalDate.now(), ef, mp);
    }

    public List<VentaResumenDTO> obtenerResumenSemanal(String tenantId) {
        LocalDateTime haceSieteDias = LocalDateTime.now().minusDays(7);
        List<Venta> ventas = ventaRepository.findByTenantIdAndFechaAfter(tenantId, haceSieteDias);

        Map<LocalDate, List<Venta>> agrupadas = ventas.stream()
                .filter(v -> !"ANULADA".equals(v.getEstado()))
                .collect(Collectors.groupingBy(v -> v.getFecha().toLocalDate()));

        Map<LocalDate, VentaResumenDTO> resumenMap = new TreeMap<>();
        agrupadas.forEach((fecha, lista) -> {
            double ef = lista.stream().filter(v -> "EFECTIVO".equalsIgnoreCase(v.getMetodoPago()))
                    .mapToDouble(Venta::getTotalFinal).sum();
            double mp = lista.stream().filter(v -> "MERCADO_PAGO".equalsIgnoreCase(v.getMetodoPago()))
                    .mapToDouble(Venta::getTotalFinal).sum();
            resumenMap.put(fecha, new VentaResumenDTO(fecha, ef, mp));
        });

        return new ArrayList<>(resumenMap.values());
    }

    private String detalleVenta(Venta venta) {
        return auditoryLogService.detalle(
                "estado", venta.getEstado(),
                "metodoPago", venta.getMetodoPago(),
                "totalFinal", venta.getTotalFinal(),
                "montoAbonado", venta.getMontoAbonado(),
                "vuelto", venta.getVuelto(),
                "items", venta.getItems() != null ? venta.getItems().size() : 0
        );
    }
}

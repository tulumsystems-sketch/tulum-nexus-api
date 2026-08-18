package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.controllers.ItemRemitoDTO;
import com.tulumcore.api.controllers.PagoRemitoDTO;
import com.tulumcore.api.controllers.PagoRemitoResponseDTO;
import com.tulumcore.api.controllers.RemitoDTO;
import com.tulumcore.api.controllers.ResumenCobranzasDTO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RemitoService {

    private static final List<String> ESTADOS_PAGO_VALIDOS = List.of("IMPAGO", "PAGADO_PARCIAL", "PAGADO");
    private static final List<String> METODOS_PAGO_VALIDOS = List.of("EFECTIVO", "TRANSFERENCIA", "MERCADO_PAGO");
    /** Tolerancia en pesos para no rechazar cobros por diferencias de redondeo. */
    private static final double TOLERANCIA = 0.01;

    @Autowired private RemitoRepository remitoRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private PagoRemitoRepository pagoRemitoRepository;
    @Autowired private CajaRepository cajaRepository;
    @Autowired private CajaService cajaService;
    @Autowired private StockMovementService stockMovementService;
    @Autowired private AuditoryLogService auditoryLogService;

    public List<Remito> getAll() {
        String tenant = TenantContext.getCurrentTenant();
        return remitoRepository.findAllByTenantIdOrderByFechaDesc(tenant);
    }

    public List<Remito> getByEstado(String estado) {
        String tenant = TenantContext.getCurrentTenant();
        return remitoRepository.findAllByTenantIdAndEstadoOrderByFechaDesc(tenant, estado);
    }

    public List<Remito> getByEstadoPago(String estadoPago) {
        String tenant = TenantContext.getCurrentTenant();
        String normalizado = estadoPago != null ? estadoPago.toUpperCase() : "";
        if (!ESTADOS_PAGO_VALIDOS.contains(normalizado)) {
            throw new BusinessException("Estado de pago invalido: " + estadoPago);
        }
        return remitoRepository.findAllByTenantIdAndEstadoPagoOrderByFechaDesc(tenant, normalizado);
    }

    public Remito getById(Long id) {
        String tenant = TenantContext.getCurrentTenant();
        return remitoRepository.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Remito no encontrado con id: " + id));
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
        double totalRemito = 0.0;
        for (ItemRemitoDTO itemDto : dto.getItems()) {
            ItemRemito item = new ItemRemito();
            item.setRemito(remito);
            int cantidad = itemDto.getCantidad() != null ? itemDto.getCantidad() : 0;
            item.setCantidad(cantidad);
            item.setDescripcion(itemDto.getDescripcion());
            item.setTenantId(tenant);
            item.setPrecioUnitario(0.0);
            item.setTotalLinea(0.0);

            if (itemDto.getProductoId() != null) {
                Producto producto = productoRepository.findByIdAndTenantId(itemDto.getProductoId(), tenant)
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
                item.setProducto(producto);
                if (item.getDescripcion() == null) {
                    item.setDescripcion(producto.getNombre());
                }
                double precioUnitario = producto.getPrecio() != null ? producto.getPrecio() : 0.0;
                double totalLinea = precioUnitario * cantidad;
                item.setPrecioUnitario(precioUnitario);
                item.setTotalLinea(totalLinea);
                totalRemito += totalLinea;
            }
            items.add(item);
        }

        remito.setItems(items);
        remito.setTotal(totalRemito);
        remito.setMontoPagado(0.0);
        remito.setSaldoPendiente(totalRemito);
        remito.setEstadoPago(totalRemito > 0 ? "IMPAGO" : "PAGADO");
        Remito saved = remitoRepository.save(remito);
        auditoryLogService.registrar("CREATE", "REMITO", saved.getId(),
                "Remito #" + saved.getNroRemito() + " creado - " +
                        (saved.getNombreDestinatario() != null ? saved.getNombreDestinatario() : "Sin destinatario"),
                null, detalleRemito(saved));
        return saved;
    }

    @Transactional
    public Remito cambiarEstado(Long id, String nuevoEstado) {
        String tenant = TenantContext.getCurrentTenant();

        List<String> estadosValidos = List.of("PENDIENTE", "EN_VIAJE", "ENTREGADO", "INCIDENCIA");
        if (!estadosValidos.contains(nuevoEstado)) {
            throw new BusinessException("Estado invalido: " + nuevoEstado);
        }

        Remito remito = remitoRepository.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Remito no encontrado con id: " + id));

        if ("ENTREGADO".equals(remito.getEstado())) {
            throw new BusinessException("Un remito entregado no puede cambiar de estado.");
        }

        if ("ENTREGADO".equals(nuevoEstado)) {
            validarStockParaEntrega(remito);
        }

        String estadoAnterior = remito.getEstado();
        String detalleAnterior = detalleRemito(remito);
        remito.setEstado(nuevoEstado);
        Remito saved = remitoRepository.save(remito);

        auditoryLogService.registrar("UPDATE", "REMITO", saved.getId(),
                "Remito #" + saved.getNroRemito() + " cambio de " + estadoAnterior + " a " + nuevoEstado,
                detalleAnterior, detalleRemito(saved));

        if ("ENTREGADO".equals(nuevoEstado)) {
            Usuario usuario = stockMovementService.getCurrentUser();
            for (ItemRemito item : saved.getItems()) {
                if (item.getProducto() != null) {
                    stockMovementService.registrar(MovementType.TRANSFERENCIA, item.getProducto(), usuario,
                            item.getCantidad(), "Remito #" + saved.getNroRemito(), null, null, saved);
                }
            }
        }

        return saved;
    }

    /** Historial de cobranzas de un remito, de la mas reciente a la mas vieja. */
    public List<PagoRemitoResponseDTO> getPagos(Long remitoId) {
        String tenant = TenantContext.getCurrentTenant();
        Remito remito = remitoRepository.findByIdAndTenantId(remitoId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Remito no encontrado con id: " + remitoId));
        return pagoRemitoRepository.findAllByTenantIdAndRemitoIdOrderByFechaDesc(tenant, remito.getId())
                .stream()
                .map(PagoRemitoResponseDTO::desde)
                .collect(Collectors.toList());
    }

    /**
     * Registra una cobranza total o parcial sobre un remito y la imputa a la caja abierta
     * en los buckets de cobranzas, separados de las ventas del dia.
     */
    @Transactional
    public Remito registrarPago(Long remitoId, PagoRemitoDTO dto) {
        String tenant = TenantContext.getCurrentTenant();

        Remito remito = remitoRepository.findByIdAndTenantId(remitoId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Remito no encontrado con id: " + remitoId));

        normalizarCobranza(remito);

        double monto = dto != null && dto.getMonto() != null ? dto.getMonto() : 0.0;
        if (monto <= 0) {
            throw new BusinessException("El monto del pago debe ser mayor a cero.");
        }

        String metodoPago = dto != null && dto.getMetodoPago() != null
                ? dto.getMetodoPago().toUpperCase()
                : "EFECTIVO";
        if (!METODOS_PAGO_VALIDOS.contains(metodoPago)) {
            throw new BusinessException("Metodo de pago invalido: " + metodoPago);
        }

        double total = nz(remito.getTotal());
        if (total <= 0) {
            throw new BusinessException("El remito no tiene un total a cobrar.");
        }

        double saldo = nz(remito.getSaldoPendiente());
        if (saldo <= TOLERANCIA) {
            throw new BusinessException("El remito #" + remito.getNroRemito() + " ya esta cobrado en su totalidad.");
        }
        if (monto > saldo + TOLERANCIA) {
            throw new BusinessException("El pago supera el saldo pendiente del remito. Saldo: "
                    + String.format("%.2f", saldo) + ".");
        }

        Caja caja = cajaRepository.findByEstadoAndTenantId("ABIERTA", tenant).orElse(null);
        if (caja == null && "EFECTIVO".equals(metodoPago)) {
            throw new BusinessException("Debe abrir caja para registrar cobranzas en efectivo.");
        }

        Usuario usuario = null;
        try {
            usuario = auditoryLogService.getCurrentUser();
        } catch (RuntimeException ignored) {
            // Flujos tecnicos sin usuario persistido igual pueden registrar la cobranza.
        }

        PagoRemito pago = new PagoRemito();
        pago.setTenantId(tenant);
        pago.setRemito(remito);
        pago.setFecha(LocalDateTime.now());
        pago.setMonto(monto);
        pago.setMetodoPago(metodoPago);
        pago.setObservaciones(dto != null ? dto.getObservaciones() : null);
        pago.setUsuario(usuario);
        PagoRemito pagoGuardado = pagoRemitoRepository.save(pago);

        String detalleAnterior = detalleRemito(remito);

        double pagadoNuevo = nz(remito.getMontoPagado()) + monto;
        remito.setMontoPagado(redondear(pagadoNuevo));
        remito.setSaldoPendiente(redondear(Math.max(0.0, total - pagadoNuevo)));
        remito.setEstadoPago(calcularEstadoPago(total, pagadoNuevo));
        Remito saved = remitoRepository.save(remito);

        if (caja != null) {
            if ("EFECTIVO".equals(metodoPago)) {
                caja.setMontoCobranzasEfectivo(redondear(nz(caja.getMontoCobranzasEfectivo()) + monto));
            } else {
                caja.setMontoCobranzasTransferencia(redondear(nz(caja.getMontoCobranzasTransferencia()) + monto));
            }
            cajaService.recalcularMontoFinalEsperado(caja);
            cajaRepository.save(caja);
        }

        auditoryLogService.registrar("CREATE", "PAGO_REMITO", pagoGuardado.getId(),
                "Cobranza de $" + String.format("%.2f", monto) + " (" + metodoPago + ") sobre remito #"
                        + saved.getNroRemito(),
                detalleAnterior, detalleRemito(saved));

        return saved;
    }

    /** Totales de cuentas por cobrar para el tablero de cobranzas. */
    public ResumenCobranzasDTO getResumenCobranzas() {
        String tenant = TenantContext.getCurrentTenant();
        List<Remito> remitos = remitoRepository.findAllByTenantIdOrderByFechaDesc(tenant);

        ResumenCobranzasDTO resumen = new ResumenCobranzasDTO();
        double facturado = 0.0;
        double cobrado = 0.0;
        double pendiente = 0.0;
        long impagos = 0;
        long parciales = 0;
        long pagados = 0;

        for (Remito remito : remitos) {
            double total = nz(remito.getTotal());
            double pagado = nz(remito.getMontoPagado());
            String estadoPago = remito.getEstadoPago() != null
                    ? remito.getEstadoPago()
                    : calcularEstadoPago(total, pagado);

            facturado += total;
            cobrado += pagado;
            pendiente += remito.getSaldoPendiente() != null
                    ? remito.getSaldoPendiente()
                    : Math.max(0.0, total - pagado);

            if ("PAGADO".equals(estadoPago)) {
                pagados++;
            } else if ("PAGADO_PARCIAL".equals(estadoPago)) {
                parciales++;
            } else {
                impagos++;
            }
        }

        resumen.setCantidadRemitos(remitos.size());
        resumen.setCantidadImpagos(impagos);
        resumen.setCantidadParciales(parciales);
        resumen.setCantidadPagados(pagados);
        resumen.setTotalFacturado(redondear(facturado));
        resumen.setTotalCobrado(redondear(cobrado));
        resumen.setTotalPendiente(redondear(pendiente));
        return resumen;
    }

    /** Completa los campos de cobranza de remitos viejos que puedan tener valores nulos. */
    private void normalizarCobranza(Remito remito) {
        double total = nz(remito.getTotal());
        if (remito.getMontoPagado() == null) {
            remito.setMontoPagado(0.0);
        }
        if (remito.getSaldoPendiente() == null) {
            remito.setSaldoPendiente(Math.max(0.0, total - remito.getMontoPagado()));
        }
        if (remito.getEstadoPago() == null || !ESTADOS_PAGO_VALIDOS.contains(remito.getEstadoPago())) {
            remito.setEstadoPago(calcularEstadoPago(total, remito.getMontoPagado()));
        }
    }

    private String calcularEstadoPago(double total, double pagado) {
        if (total <= 0 || pagado >= total - TOLERANCIA) {
            return "PAGADO";
        }
        return pagado <= TOLERANCIA ? "IMPAGO" : "PAGADO_PARCIAL";
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private double nz(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private String generarNroRemito() {
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = remitoRepository.count() + 1;
        return "R-" + fecha + "-" + String.format("%04d", count);
    }

    private void validarStockParaEntrega(Remito remito) {
        Map<Long, Integer> cantidadesPorProducto = new HashMap<>();
        Map<Long, Producto> productos = new HashMap<>();

        for (ItemRemito item : remito.getItems()) {
            Producto producto = item.getProducto();
            if (producto == null) {
                continue;
            }

            int cantidad = item.getCantidad() != null ? item.getCantidad() : 0;
            if (cantidad <= 0) {
                throw new BusinessException("La cantidad del remito debe ser mayor a cero para " + producto.getNombre() + ".");
            }

            cantidadesPorProducto.merge(producto.getId(), cantidad, Integer::sum);
            productos.put(producto.getId(), producto);
        }

        for (Map.Entry<Long, Integer> entry : cantidadesPorProducto.entrySet()) {
            Producto producto = productos.get(entry.getKey());
            int disponible = producto.getCantidadStock() != null ? producto.getCantidadStock() : 0;
            int requerido = entry.getValue();
            if (disponible < requerido) {
                throw new BusinessException("Stock insuficiente para entregar remito. Producto: "
                        + producto.getNombre() + ". Disponible: " + disponible + ", requerido: " + requerido + ".");
            }
        }
    }

    private String detalleRemito(Remito remito) {
        return auditoryLogService.detalle(
                "nroRemito", remito.getNroRemito(),
                "estado", remito.getEstado(),
                "destinatario", remito.getNombreDestinatario(),
                "direccionEntrega", remito.getDireccionEntrega(),
                "total", remito.getTotal(),
                "estadoPago", remito.getEstadoPago(),
                "montoPagado", remito.getMontoPagado(),
                "saldoPendiente", remito.getSaldoPendiente(),
                "items", remito.getItems() != null ? remito.getItems().size() : 0
        );
    }
}

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

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException("El remito debe tener al menos un item.");
        }

        if (dto.getClienteId() != null) {
            Cliente cliente = clienteRepository.findByIdAndTenantId(dto.getClienteId(), tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
            remito.setCliente(cliente);
        }

        List<ItemRemito> items = new ArrayList<>();
        double totalRemito = 0.0;
        for (ItemRemitoDTO itemDto : dto.getItems()) {
            ItemRemito item = armarItem(itemDto, remito, tenant);
            totalRemito += nz(item.getTotalLinea());
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
    public Remito actualizar(Long id, RemitoDTO dto) {
        String tenant = TenantContext.getCurrentTenant();
        Remito remito = remitoRepository.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Remito no encontrado con id: " + id));

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException("El remito debe tener al menos un item.");
        }

        String detalleAnterior = detalleRemito(remito);
        Map<Long, Double> stockAnterior = cantidadesPorProducto(remito);
        boolean yaEntregado = "ENTREGADO".equals(remito.getEstado());

        remito.setDireccionEntrega(dto.getDireccionEntrega());
        remito.setNombreDestinatario(dto.getNombreDestinatario());
        remito.setTelefonoDestinatario(dto.getTelefonoDestinatario());
        remito.setObservaciones(dto.getObservaciones());

        if (dto.getClienteId() != null) {
            Cliente cliente = clienteRepository.findByIdAndTenantId(dto.getClienteId(), tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
            remito.setCliente(cliente);
        } else {
            remito.setCliente(null);
        }

        if (remito.getItems() == null) {
            remito.setItems(new ArrayList<>());
        } else {
            remito.getItems().clear();
        }

        double totalRemito = 0.0;
        for (ItemRemitoDTO itemDto : dto.getItems()) {
            ItemRemito item = armarItem(itemDto, remito, tenant);
            totalRemito += nz(item.getTotalLinea());
            remito.getItems().add(item);
        }

        remito.setTotal(totalRemito);
        double pagado = nz(remito.getMontoPagado());
        if (pagado > totalRemito + TOLERANCIA) {
            throw new BusinessException("El remito ya tiene cobros por "
                    + String.format("%.2f", pagado)
                    + " y el nuevo total es menor. Ajusta los cobros antes de editar.");
        }
        remito.setSaldoPendiente(Math.max(0, totalRemito - pagado));
        if (totalRemito <= TOLERANCIA) {
            remito.setEstadoPago("PAGADO");
        } else if (pagado <= TOLERANCIA) {
            remito.setEstadoPago("IMPAGO");
        } else if (pagado + TOLERANCIA >= totalRemito) {
            remito.setEstadoPago("PAGADO");
        } else {
            remito.setEstadoPago("PAGADO_PARCIAL");
        }

        if (yaEntregado) {
            validarStockParaEntrega(remito, stockAnterior);
        }

        Remito saved = remitoRepository.save(remito);

        if (yaEntregado) {
            ajustarStockPorEdicion(saved, stockAnterior);
        }

        auditoryLogService.registrar("UPDATE", "REMITO", saved.getId(),
                "Remito #" + saved.getNroRemito() + " editado",
                detalleAnterior, detalleRemito(saved));
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

        Caja caja = cajaService.obtenerCajaAbiertaActualizada().orElse(null);
        if (caja == null && "EFECTIVO".equals(metodoPago)) {
            throw new BusinessException("Debe abrir caja para registrar cobranzas en efectivo. "
                    + "Si el turno anterior cumplió el día, se cerró automáticamente.");
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
            cajaService.reconstruirTurno(caja);
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

    private ItemRemito armarItem(ItemRemitoDTO itemDto, Remito remito, String tenant) {
        double cantidad = itemDto.getCantidad() != null ? itemDto.getCantidad() : 0;
        if (cantidad <= 0) {
            throw new BusinessException("La cantidad debe ser mayor a cero.");
        }

        ItemRemito item = new ItemRemito();
        item.setRemito(remito);
        item.setTenantId(tenant);
        item.setCantidad(cantidad);
        item.setDescripcion(itemDto.getDescripcion());

        if (itemDto.getProductoId() != null) {
            Producto producto = productoRepository.findByIdAndTenantId(itemDto.getProductoId(), tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
            item.setProducto(producto);
            double precio = producto.getPrecio() != null ? producto.getPrecio() : 0;
            item.setPrecioUnitario(precio);
            item.setTotalLinea(redondear(precio * cantidad));
        } else {
            item.setPrecioUnitario(0.0);
            item.setTotalLinea(0.0);
        }
        return item;
    }

    private Map<Long, Double> cantidadesPorProducto(Remito remito) {
        Map<Long, Double> cantidades = new HashMap<>();
        if (remito.getItems() == null) {
            return cantidades;
        }
        for (ItemRemito item : remito.getItems()) {
            if (item.getProducto() == null) {
                continue;
            }
            cantidades.merge(item.getProducto().getId(), nz(item.getCantidad()), Double::sum);
        }
        return cantidades;
    }

    private void validarStockParaEntrega(Remito remito) {
        validarStockParaEntrega(remito, Map.of());
    }

    private void validarStockParaEntrega(Remito remito, Map<Long, Double> stockYaDescontado) {
        Map<Long, Double> requerido = cantidadesPorProducto(remito);
        Map<Long, Producto> productos = new HashMap<>();
        if (remito.getItems() != null) {
            for (ItemRemito item : remito.getItems()) {
                if (item.getProducto() != null) {
                    productos.put(item.getProducto().getId(), item.getProducto());
                }
            }
        }

        for (Map.Entry<Long, Double> entry : requerido.entrySet()) {
            Producto producto = productos.get(entry.getKey());
            if (producto == null) {
                producto = productoRepository.findByIdAndTenantId(entry.getKey(), remito.getTenantId())
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
            }
            double disponible = nz(producto.getCantidadStock()) + stockYaDescontado.getOrDefault(entry.getKey(), 0.0);
            double pedido = entry.getValue();
            if (pedido <= 0) {
                throw new BusinessException("La cantidad del remito debe ser mayor a cero para " + producto.getNombre() + ".");
            }
            if (disponible + 0.0001 < pedido) {
                throw new BusinessException("Stock insuficiente para entregar remito. Producto: "
                        + producto.getNombre() + ". Disponible: " + disponible + ", requerido: " + pedido + ".");
            }
        }
    }

    private void ajustarStockPorEdicion(Remito remito, Map<Long, Double> stockAnterior) {
        Map<Long, Double> stockNuevo = cantidadesPorProducto(remito);
        Map<Long, Producto> productos = new HashMap<>();
        if (remito.getItems() != null) {
            for (ItemRemito item : remito.getItems()) {
                if (item.getProducto() != null) {
                    productos.put(item.getProducto().getId(), item.getProducto());
                }
            }
        }

        java.util.Set<Long> ids = new java.util.HashSet<>();
        ids.addAll(stockAnterior.keySet());
        ids.addAll(stockNuevo.keySet());

        Usuario usuario = stockMovementService.getCurrentUser();
        for (Long productoId : ids) {
            double anterior = stockAnterior.getOrDefault(productoId, 0.0);
            double actual = stockNuevo.getOrDefault(productoId, 0.0);
            double delta = actual - anterior;
            if (Math.abs(delta) < 0.0001) {
                continue;
            }
            Producto producto = productos.get(productoId);
            if (producto == null) {
                producto = productoRepository.findByIdAndTenantId(productoId, remito.getTenantId())
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
            }
            if (delta > 0) {
                stockMovementService.registrar(MovementType.TRANSFERENCIA, producto, usuario,
                        delta, "Edicion remito #" + remito.getNroRemito() + " (mas kg)", null, null, remito);
            } else {
                stockMovementService.registrar(MovementType.AJUSTE, producto, usuario,
                        Math.abs(delta), "Edicion remito #" + remito.getNroRemito() + " (devuelve stock)",
                        null, null, remito);
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

package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.controllers.ItemVentaDTO;
import com.tulumcore.api.controllers.VentaDTO;
import com.tulumcore.api.controllers.VentaListadoDTO;
import com.tulumcore.api.controllers.VentaResumenDTO;
import com.tulumcore.api.controllers.VentaTotalesDTO;
import com.tulumcore.api.entities.*;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.repositories.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VentaService {

    public static final String EFECTIVO = "EFECTIVO";
    public static final String TRANSFERENCIA = "TRANSFERENCIA";
    public static final String MERCADO_PAGO = "MERCADO_PAGO";

    /** IVA aplicado cuando el tenant todavía no tiene una fila de configuración. */
    private static final double IVA_POR_DEFECTO = 21.0;

    @Autowired private VentaRepository ventaRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private CajaRepository cajaRepository;
    @Autowired private TenantConfigRepository tenantConfigRepository;
    @Autowired private CajaService cajaService;
    @Autowired private StockMovementService stockMovementService;
    @Autowired private AuditoryLogService auditoryLogService;
    @Autowired private UsuarioRepository usuarioRepository;

    @Transactional
    public Venta guardar(VentaDTO dto) {
        if (dto == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException("El pedido necesita al menos un producto.");
        }

        String tenant = TenantContext.getCurrentTenant();
        String canal = CanalVenta.normalizar(dto.getCanal());
        Caja caja = cajaService.exigirCajaOperativa();

        TenantConfig config = tenantConfigRepository.findByTenantId(tenant).orElse(null);

        Venta venta = new Venta();
        venta.setObservaciones(dto.getObservaciones());
        venta.setMoneda("ARS");
        venta.setMetodoPago(normalizarMetodoPago(dto.getMetodoPago(), config));
        venta.setTenantId(tenant);
        venta.setCanal(canal);
        venta.setNombreContacto(textoOpcional(dto.getNombreContacto()));
        venta.setTelefonoContacto(textoOpcional(dto.getTelefonoContacto()));
        venta.setDireccionEntrega(textoOpcional(dto.getDireccionEntrega()));

        if (dto.getClienteId() != null && dto.getClienteId() > 0) {
            Cliente cliente = clienteRepository.findByIdAndTenantId(dto.getClienteId(), tenant).orElse(null);
            venta.setCliente(cliente);
            completarContactoDesdeCliente(venta, cliente);
        }

        if (CanalVenta.esPedido(canal)
                && textoOpcional(venta.getTelefonoContacto()) == null
                && textoOpcional(venta.getNombreContacto()) == null) {
            throw new BusinessException("Indicá un cliente, o al menos nombre o teléfono del pedido.");
        }

        List<ItemVenta> items = new ArrayList<>();
        Map<Long, Integer> cantidadesPorProducto = new HashMap<>();
        double subtotal = 0;

        Usuario usuario = resolverUsuarioMovimiento(tenant);

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

        // El IVA sale de la configuración del tenant: 0 significa que no se discrimina
        // y el total final queda igual al subtotal.
        double ivaPorcentaje = ivaPorcentaje(config);
        double totalIva = subtotal * ivaPorcentaje / 100.0;
        double totalFinal = subtotal + totalIva;

        venta.setItems(items);
        venta.setTotalNeto(subtotal);
        venta.setTotalIva(totalIva);
        venta.setTotalFinal(totalFinal);
        venta.setEstado(EstadoPedido.inicialParaCanal(canal));
        if (EFECTIVO.equals(venta.getMetodoPago())) {
            double abonado = dto.getMontoAbonado() != null ? dto.getMontoAbonado() : totalFinal;
            venta.setMontoAbonado(abonado);
            venta.setVuelto(Math.max(0, abonado - totalFinal));
        }

        Venta saved = ventaRepository.save(venta);
        if (saved.getNroComprobante() == null || saved.getNroComprobante().isBlank()) {
            String prefijo = CanalVenta.esPedido(canal) ? "P-" : "V-";
            saved.setNroComprobante(prefijo + saved.getId());
            saved = ventaRepository.save(saved);
        }
        cajaService.reconstruirTurno(caja);

        for (ItemVenta item : saved.getItems()) {
            stockMovementService.registrar(MovementType.VENTA, item.getProducto(), usuario,
                    item.getCantidad(), "Venta #" + saved.getId(), saved, null);
        }

        String clienteNombre = saved.getCliente() != null
                ? saved.getCliente().getNombre() + " " + saved.getCliente().getApellido()
                : (saved.getNombreContacto() != null ? saved.getNombreContacto() : "Consumidor Final");
        auditoryLogService.registrar("CREATE", "VENTA", saved.getId(),
                "Venta #" + saved.getId() + " - " + clienteNombre + " - $" +
                        String.format("%.2f", saved.getTotalFinal()) + " (" + saved.getMetodoPago() + "/" + saved.getCanal() + ")",
                null, detalleVenta(saved));

        return saved;
    }

    @Transactional
    public Venta actualizarEstado(Long id, String nuevoEstado) {
        String tenant = TenantContext.getCurrentTenant();
        Venta venta = ventaRepository.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con id: " + id));
        EstadoPedido.validarTransicion(venta.getEstado(), nuevoEstado);
        String detalleAnterior = detalleVenta(venta);
        venta.setEstado(EstadoPedido.normalizar(nuevoEstado));
        Venta saved = ventaRepository.save(venta);
        auditoryLogService.registrar("UPDATE", "VENTA", saved.getId(),
                "Pedido #" + saved.getId() + " pasó a " + saved.getEstado(),
                detalleAnterior, detalleVenta(saved));
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

        venta.setEstado("ANULADA");
        Venta saved = ventaRepository.save(venta);
        cajaRepository.findByEstadoAndTenantId("ABIERTA", tenant)
                .ifPresent(cajaService::reconstruirTurno);
        String clienteNombre = saved.getCliente() != null
                ? saved.getCliente().getNombre() + " " + saved.getCliente().getApellido()
                : "Consumidor Final";
        auditoryLogService.registrar("UPDATE", "VENTA", saved.getId(),
                "Venta #" + saved.getId() + " anulada - " + clienteNombre, detalleAnterior, detalleVenta(saved));
        return saved;
    }

    public Page<VentaListadoDTO> buscarVentas(String tenantId, LocalDate desde, LocalDate hasta,
                                    String metodoPago, String estado, Long clienteId,
                                    boolean soloWhatsapp, Pageable pageable) {
        return buscarVentas(tenantId, desde, hasta, metodoPago, estado, clienteId,
                soloWhatsapp, null, false, pageable);
    }

    public Page<VentaListadoDTO> buscarVentas(String tenantId, LocalDate desde, LocalDate hasta,
                                    String metodoPago, String estado, Long clienteId,
                                    boolean soloWhatsapp, String canal, boolean soloPedidos,
                                    Pageable pageable) {
        Pageable ordenado = pageable;
        if (pageable == null || pageable.getSort().isUnsorted()) {
            int page = pageable != null ? pageable.getPageNumber() : 0;
            int size = pageable != null ? pageable.getPageSize() : 20;
            ordenado = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fecha"));
        }
        Page<Venta> page = ventaRepository.findAll((Specification<Venta>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (desde != null) predicates.add(cb.greaterThanOrEqualTo(root.get("fecha"), desde.atStartOfDay()));
            if (hasta != null) predicates.add(cb.lessThanOrEqualTo(root.get("fecha"), hasta.atTime(23, 59, 59)));
            if (metodoPago != null && !metodoPago.isEmpty()) predicates.add(cb.equal(root.get("metodoPago"), metodoPago));
            if (estado != null && !estado.isEmpty()) predicates.add(cb.equal(root.get("estado"), estado));
            if (clienteId != null) predicates.add(cb.equal(root.get("cliente").get("id"), clienteId));
            aplicarFiltroCanal(predicates, cb, root, canal, soloPedidos, soloWhatsapp);
            return cb.and(predicates.toArray(new Predicate[0]));
        }, ordenado);
        Map<Long, Venta> conItems = hidratarItems(page.getContent());
        return page.map(venta -> VentaListadoDTO.desde(conItems.getOrDefault(venta.getId(), venta)));
    }

    public List<VentaListadoDTO> getAllVentas(String tenantId) {
        return buscarVentas(tenantId, null, null, null, null, null, false,
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "fecha"))).getContent();
    }

    @Transactional(readOnly = true)
    public Venta obtenerDetalle(Long id) {
        String tenant = TenantContext.getCurrentTenant();
        return ventaRepository.findDetalleByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con id: " + id));
    }

    public VentaTotalesDTO obtenerTotales(String tenantId) {
        List<Object[]> filas = ventaRepository.totalesNoAnuladas(tenantId);
        if (filas == null || filas.isEmpty() || filas.get(0) == null) {
            return new VentaTotalesDTO(0, 0);
        }
        Object[] fila = filas.get(0);
        long cantidad = fila[0] instanceof Number ? ((Number) fila[0]).longValue() : 0;
        double ingresos = fila[1] instanceof Number ? ((Number) fila[1]).doubleValue() : 0;
        return new VentaTotalesDTO(cantidad, ingresos);
    }

    public VentaResumenDTO obtenerResumenHoy(String tenantId) {
        LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
        List<Venta> ventas = ventaRepository.findByTenantIdAndFechaAfter(tenantId, inicioHoy);

        List<Venta> validas = ventas.stream()
                .filter(v -> !"ANULADA".equals(v.getEstado()))
                .toList();

        return new VentaResumenDTO(LocalDate.now(),
                totalPorMetodo(validas, EFECTIVO),
                totalPorMetodo(validas, MERCADO_PAGO),
                totalPorMetodo(validas, TRANSFERENCIA));
    }

    public List<VentaResumenDTO> obtenerResumenSemanal(String tenantId) {
        LocalDateTime haceSieteDias = LocalDateTime.now().minusDays(7);
        List<Venta> ventas = ventaRepository.findByTenantIdAndFechaAfter(tenantId, haceSieteDias);

        Map<LocalDate, List<Venta>> agrupadas = ventas.stream()
                .filter(v -> !"ANULADA".equals(v.getEstado()))
                .collect(Collectors.groupingBy(v -> v.getFecha().toLocalDate()));

        Map<LocalDate, VentaResumenDTO> resumenMap = new TreeMap<>();
        agrupadas.forEach((fecha, lista) -> resumenMap.put(fecha, new VentaResumenDTO(fecha,
                totalPorMetodo(lista, EFECTIVO),
                totalPorMetodo(lista, MERCADO_PAGO),
                totalPorMetodo(lista, TRANSFERENCIA))));

        return new ArrayList<>(resumenMap.values());
    }

    private double totalPorMetodo(List<Venta> ventas, String metodoPago) {
        return ventas.stream()
                .filter(v -> metodoPago.equals(normalizarMetodoPago(v.getMetodoPago(), null)))
                .mapToDouble(v -> v.getTotalFinal() != null ? v.getTotalFinal() : 0)
                .sum();
    }

    private double ivaPorcentaje(TenantConfig config) {
        return config != null ? config.getIvaPorcentaje() : IVA_POR_DEFECTO;
    }

    /**
     * Deja el método de pago en uno de los tres valores canónicos.
     * Cuando no llega informado se elige el primero habilitado para el tenant, priorizando
     * Mercado Pago para no cambiar el comportamiento histórico de quienes lo tienen activo.
     */
    private String normalizarMetodoPago(String metodoPago, TenantConfig config) {
        if (metodoPago != null && !metodoPago.isBlank()) {
            String normalizado = metodoPago.trim().toUpperCase();
            if (EFECTIVO.equals(normalizado) || TRANSFERENCIA.equals(normalizado)) {
                return normalizado;
            }
            return MERCADO_PAGO;
        }

        if (config == null || config.isPagoMercadoPagoHabilitado()) {
            return MERCADO_PAGO;
        }
        if (config.isPagoEfectivoHabilitado()) {
            return EFECTIVO;
        }
        return config.isPagoTransferenciaHabilitado() ? TRANSFERENCIA : EFECTIVO;
    }

    private String detalleVenta(Venta venta) {
        return auditoryLogService.detalle(
                "estado", venta.getEstado(),
                "canal", venta.getCanal(),
                "metodoPago", venta.getMetodoPago(),
                "totalFinal", venta.getTotalFinal(),
                "montoAbonado", venta.getMontoAbonado(),
                "vuelto", venta.getVuelto(),
                "items", venta.getItems() != null ? venta.getItems().size() : 0
        );
    }

    private void aplicarFiltroCanal(List<Predicate> predicates, jakarta.persistence.criteria.CriteriaBuilder cb,
                                    jakarta.persistence.criteria.Root<Venta> root,
                                    String canal, boolean soloPedidos, boolean soloWhatsapp) {
        if (canal != null && !canal.isBlank()) {
            predicates.add(cb.equal(root.get("canal"), CanalVenta.normalizar(canal)));
            return;
        }
        if (soloPedidos) {
            predicates.add(root.get("canal").in(CanalVenta.WHATSAPP, CanalVenta.DELIVERY));
            return;
        }
        if (soloWhatsapp) {
            predicates.add(cb.equal(root.get("canal"), CanalVenta.WHATSAPP));
        }
    }

    private Map<Long, Venta> hidratarItems(List<Venta> ventas) {
        if (ventas == null || ventas.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = ventas.stream().map(Venta::getId).toList();
        return ventaRepository.findWithItemsByIdIn(ids).stream()
                .collect(Collectors.toMap(Venta::getId, v -> v, (a, b) -> a));
    }

    private Usuario resolverUsuarioMovimiento(String tenant) {
        try {
            return stockMovementService.getCurrentUser();
        } catch (RuntimeException ignored) {
            // El bot no es un usuario de login: usamos el admin del comercio para el movimiento de stock.
        }
        return usuarioRepository.findAllByTenantId(tenant).stream()
                .filter(u -> u.getRol() == Rol.ADMIN || u.getRol() == Rol.OPERADOR)
                .min(Comparator.comparing(Usuario::getId))
                .orElseThrow(() -> new BusinessException(
                        "No hay un usuario del comercio para registrar el movimiento de stock."));
    }

    private void completarContactoDesdeCliente(Venta venta, Cliente cliente) {
        if (cliente == null) {
            return;
        }
        if (textoOpcional(venta.getNombreContacto()) == null) {
            String nombre = ((cliente.getNombre() != null ? cliente.getNombre() : "")
                    + " " + (cliente.getApellido() != null ? cliente.getApellido() : "")).trim();
            venta.setNombreContacto(textoOpcional(nombre));
        }
        if (textoOpcional(venta.getTelefonoContacto()) == null) {
            venta.setTelefonoContacto(textoOpcional(cliente.getTelefono()));
        }
        if (textoOpcional(venta.getDireccionEntrega()) == null) {
            venta.setDireccionEntrega(textoOpcional(cliente.getDireccion()));
        }
    }

    private String textoOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }
}

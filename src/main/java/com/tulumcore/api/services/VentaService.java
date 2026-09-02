package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.controllers.BotCadeteDTO;
import com.tulumcore.api.controllers.BotEnviosDTO;
import com.tulumcore.api.controllers.BotEquipoDTO;
import com.tulumcore.api.controllers.DespachoDTO;
import com.tulumcore.api.controllers.ItemVentaDTO;
import com.tulumcore.api.controllers.MesaDivisionDTO;
import com.tulumcore.api.controllers.SalidaPedidosDTO;
import com.tulumcore.api.controllers.VentaCobroDTO;
import com.tulumcore.api.controllers.VentaDTO;
import com.tulumcore.api.controllers.VentaListadoDTO;
import com.tulumcore.api.controllers.VentaResumenDTO;
import com.tulumcore.api.controllers.VentaTotalesDTO;
import com.tulumcore.api.entities.*;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.repositories.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    @Autowired private MesaRepository mesaRepository;
    @Autowired private CajaService cajaService;
    @Autowired private StockMovementService stockMovementService;
    @Autowired private AuditoryLogService auditoryLogService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RecetaService recetaService;

    @Transactional
    public Venta guardar(VentaDTO dto) {
        if (dto == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException("El pedido necesita al menos un producto.");
        }

        String tenant = TenantContext.getCurrentTenant();
        String canal = CanalVenta.normalizar(dto.getCanal());
        Caja caja = CanalVenta.WHATSAPP.equals(canal) && "fogon".equalsIgnoreCase(tenant)
                ? cajaService.exigirCajaOperativaOAbrirParaWhatsApp()
                : cajaService.exigirCajaOperativa();

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
        venta.setCobrado(CanalVenta.esCuentaAbierta(canal)
                ? Boolean.TRUE.equals(dto.getCobrado())
                : true);

        if (dto.getClienteId() != null && dto.getClienteId() > 0) {
            Cliente cliente = clienteRepository.findByIdAndTenantId(dto.getClienteId(), tenant).orElse(null);
            venta.setCliente(cliente);
            completarContactoDesdeCliente(venta, cliente);
        }

        if (dto.getMesaId() != null) {
            Mesa mesa = mesaRepository.findByIdAndTenantId(dto.getMesaId(), tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada: " + dto.getMesaId()));
            if (!mesa.isActiva()) {
                throw new BusinessException("La mesa está desactivada.");
            }
            if (!CanalVenta.esSalon(canal)) {
                throw new BusinessException("Solo las cuentas de salón se vinculan a una mesa.");
            }
            asegurarMesaLibre(mesa, null);
            venta.setMesa(mesa);
        }

        if (CanalVenta.esPedido(canal)
                && textoOpcional(venta.getTelefonoContacto()) == null
                && textoOpcional(venta.getNombreContacto()) == null) {
            throw new BusinessException("Indicá un cliente, o al menos nombre o teléfono del pedido.");
        }
        CanalVenta.exigirDireccionSiDelivery(canal, venta.getDireccionEntrega());

        List<ItemVenta> items = new ArrayList<>();
        Map<Long, Integer> cantidadesPorProducto = new HashMap<>();
        Map<Long, Producto> productosPorId = new HashMap<>();
        double subtotal = 0;

        Usuario usuario = resolverUsuarioMovimiento(tenant);

        if (dto.getItems() != null) {
        for (ItemVentaDTO itemDto : dto.getItems()) {
            Producto p = productoRepository.findByIdAndTenantId(itemDto.getProductoId(), tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + itemDto.getProductoId()));
            if (!p.isVendible()) {
                throw new BusinessException(p.getNombre() + " es de depósito. No se vende: usalo en una receta o marcalo como vendible.");
            }

            int cantidad = itemDto.getCantidad() != null ? itemDto.getCantidad() : 0;
            if (cantidad <= 0) {
                throw new BusinessException("La cantidad vendida debe ser mayor a cero para: " + p.getNombre());
            }

            cantidadesPorProducto.merge(p.getId(), cantidad, Integer::sum);
            productosPorId.put(p.getId(), p);

            ItemVenta item = new ItemVenta();
            item.setVenta(venta);
            item.setProducto(p);
            item.setCantidad(cantidad);
            item.setPrecioUnitario(p.getPrecio());
            item.setObservaciones(textoOpcional(itemDto.getObservaciones()));
            item.setTenantId(tenant);
            items.add(item);
            subtotal += p.getPrecio() * cantidad;
        }
        }
        recetaService.exigirDisponible(productosPorId, cantidadesPorProducto);

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
            String prefijo = CanalVenta.esSalon(canal) ? "M-"
                    : (CanalVenta.esPedido(canal) ? "P-" : "V-");
            saved.setNroComprobante(prefijo + saved.getId());
            saved = ventaRepository.save(saved);
        }
        if (saved.getMesa() != null) {
            marcarMesaOcupada(saved.getMesa());
        }
        cajaService.reconstruirTurno(caja);

        for (ItemVenta item : saved.getItems()) {
            recetaService.aplicarVenta(item.getProducto(), item.getCantidad(), usuario, saved, false);
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
        Usuario actor = usuarioActualOpcional();
        String estadoNormalizado = EstadoPedido.normalizar(nuevoEstado);
        restringirEstadoSiRepartidor(actor, venta, estadoNormalizado);
        if (CanalVenta.esSalon(venta.getCanal())
                && (EstadoPedido.EN_CAMINO.equals(estadoNormalizado)
                || EstadoPedido.ENTREGADO.equals(estadoNormalizado))) {
            throw new BusinessException("La comanda de mesa no sale a la calle. Cuando está lista, se cobra en Mesas.");
        }
        EstadoPedido.validarTransicion(venta.getEstado(), nuevoEstado);
        String detalleAnterior = detalleVenta(venta);
        venta.setEstado(estadoNormalizado);
        Venta saved = ventaRepository.save(venta);
        if (EstadoPedido.ENTREGADO.equals(estadoNormalizado) || EstadoPedido.ANULADA.equals(estadoNormalizado)) {
            liberarMesaSiCorresponde(saved);
        }
        auditoryLogService.registrar("UPDATE", "VENTA", saved.getId(),
                "Pedido #" + saved.getId() + " pasó a " + saved.getEstado(),
                detalleAnterior, detalleVenta(saved));
        return saved;
    }

    @Transactional
    public Venta tomarPedido(Long id) {
        return tomarPedidoComo(id, exigirUsuarioLogin());
    }

    @Transactional
    public Venta despacharEnvio(Long id, DespachoDTO dto) {
        Usuario actor = exigirUsuarioLogin();
        if (actor.getRol() == Rol.REPARTIDOR) {
            return tomarPedidoComo(id, actor);
        }
        if (actor.getRol() != Rol.ADMIN && actor.getRol() != Rol.OPERADOR && actor.getRol() != Rol.SUPER_ADMIN) {
            throw new BusinessException("No podés despachar este envío.");
        }
        String tenant = TenantContext.getCurrentTenant();
        Venta venta = ventaRepository.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con id: " + id));
        if (!PedidoSalida.puedeTomar(venta.getEstado(), venta.getCanal(), venta.getDireccionEntrega(),
                venta.getRepartidorUsuarioId())) {
            throw new BusinessException("Este pedido no está listo para salir.");
        }
        Long cadeteId = dto != null ? dto.getRepartidorUsuarioId() : null;
        String detalleAnterior = detalleVenta(venta);
        if (cadeteId != null) {
            Usuario cadete = usuarioRepository.findByIdAndTenantId(cadeteId, tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Cadete no encontrado."));
            if (cadete.getRol() != Rol.REPARTIDOR) {
                throw new BusinessException("Ese usuario no es cadete.");
            }
            venta.setRepartidorUsuarioId(cadete.getId());
            venta.setRepartidorNombre(PedidoSalida.nombreVisible(cadete));
        } else {
            venta.setRepartidorUsuarioId(null);
            venta.setRepartidorNombre("Cocina / salón");
        }
        venta.setEstado(EstadoPedido.EN_CAMINO);
        Venta saved = ventaRepository.save(venta);
        auditoryLogService.registrar("UPDATE", "VENTA", saved.getId(),
                "Pedido #" + saved.getId() + " despachado"
                        + (saved.getRepartidorNombre() != null ? " · " + saved.getRepartidorNombre() : ""),
                detalleAnterior, detalleVenta(saved));
        return saved;
    }

    @Transactional
    public Venta tomarPedidoComo(Long id, Usuario cadete) {
        if (cadete == null || cadete.getRol() != Rol.REPARTIDOR) {
            throw new BusinessException("Solo un cadete puede tomar el pedido.");
        }
        String tenant = TenantContext.getCurrentTenant();
        Venta venta = resolverVentaParaTomar(id, tenant);
        Long asignado = venta.getRepartidorUsuarioId();
        if (asignado != null && !asignado.equals(cadete.getId())) {
            throw new BusinessException("Otro cadete ya tomó este pedido.");
        }
        if (PedidoSalida.puedeSalir(venta.getEstado(), venta.getCanal(), venta.getDireccionEntrega(),
                asignado, cadete.getId())) {
            String detalleAnterior = detalleVenta(venta);
            venta.setRepartidorUsuarioId(cadete.getId());
            venta.setRepartidorNombre(PedidoSalida.nombreVisible(cadete));
            venta.setEstado(EstadoPedido.EN_CAMINO);
            Venta saved = ventaRepository.save(venta);
            auditoryLogService.registrar("UPDATE", "VENTA", saved.getId(),
                    "Cadete " + saved.getRepartidorNombre() + " tomó pedido #" + saved.getId(),
                    detalleAnterior, detalleVenta(saved));
            return saved;
        }
        if (PedidoSalida.puedeReservar(venta.getEstado(), venta.getCanal(), venta.getDireccionEntrega(), asignado)
                || (asignado != null && asignado.equals(cadete.getId())
                && CanalVenta.esEnvio(venta.getCanal(), venta.getDireccionEntrega())
                && (EstadoPedido.PENDIENTE.equals(venta.getEstado())
                || EstadoPedido.EN_PREPARACION.equals(venta.getEstado())))) {
            if (asignado != null) {
                return venta;
            }
            String detalleAnterior = detalleVenta(venta);
            venta.setRepartidorUsuarioId(cadete.getId());
            venta.setRepartidorNombre(PedidoSalida.nombreVisible(cadete));
            Venta saved = ventaRepository.save(venta);
            auditoryLogService.registrar("UPDATE", "VENTA", saved.getId(),
                    "Cadete " + saved.getRepartidorNombre() + " se anotó en pedido #" + saved.getId(),
                    detalleAnterior, detalleVenta(saved));
            return saved;
        }
        throw new BusinessException("Este pedido no está listo para salir.");
    }

    @Transactional
    public Venta marcarEntregadoComo(Long id, Usuario cadete) {
        exigirCadeteDuenio(cadete, id);
        return actualizarEstado(id, EstadoPedido.ENTREGADO);
    }

    @Transactional
    public Venta actualizarCobroComo(Long id, Usuario cadete, VentaCobroDTO dto) {
        exigirCadeteDuenio(cadete, id);
        return actualizarCobro(id, dto);
    }

    @Transactional
    public Venta liberarPedido(Long id) {
        return liberarPedidoInterno(id, exigirUsuarioLogin());
    }

    @Transactional
    public Venta liberarPedidoComo(Long id, Usuario cadete) {
        exigirCadeteDuenio(cadete, id);
        return liberarPedidoInterno(id, cadete);
    }

    private Venta liberarPedidoInterno(Long id, Usuario actor) {
        if (actor == null) {
            throw new BusinessException("Necesitás iniciar sesión.");
        }
        String tenant = TenantContext.getCurrentTenant();
        Venta venta = ventaRepository.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con id: " + id));
        if (!EstadoPedido.EN_CAMINO.equals(venta.getEstado())) {
            throw new BusinessException("Solo se puede devolver a la cola un envío en camino.");
        }
        if (actor.getRol() == Rol.REPARTIDOR
                && (venta.getRepartidorUsuarioId() == null || !venta.getRepartidorUsuarioId().equals(actor.getId()))) {
            throw new BusinessException("Este envío no es tuyo.");
        }
        if (actor.getRol() != Rol.REPARTIDOR
                && actor.getRol() != Rol.ADMIN
                && actor.getRol() != Rol.OPERADOR
                && actor.getRol() != Rol.SUPER_ADMIN) {
            throw new BusinessException("No podés devolver este envío a la cola.");
        }
        String detalleAnterior = detalleVenta(venta);
        venta.setRepartidorUsuarioId(null);
        venta.setRepartidorNombre(null);
        venta.setEstado(EstadoPedido.LISTO);
        Venta saved = ventaRepository.save(venta);
        auditoryLogService.registrar("UPDATE", "VENTA", saved.getId(),
                "Pedido #" + saved.getId() + " volvió a la cola de salida",
                detalleAnterior, detalleVenta(saved));
        return saved;
    }

    /**
     * Abre una cuenta vacía en una mesa libre. El mozo ya tomó el pedido;
     * los ítems se cargan después (edición).
     */
    @Transactional
    public Venta abrirCuentaSalon(Long mesaId) {
        String tenant = TenantContext.getCurrentTenant();
        Mesa mesa = mesaRepository.findByIdAndTenantId(mesaId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada: " + mesaId));
        if (!mesa.isActiva()) {
            throw new BusinessException("La mesa está desactivada.");
        }
        asegurarMesaLibre(mesa, null);
        Caja caja = cajaService.exigirCajaOperativa();
        TenantConfig config = tenantConfigRepository.findByTenantId(tenant).orElse(null);

        Venta venta = new Venta();
        venta.setTenantId(tenant);
        venta.setCanal(CanalVenta.SALON);
        venta.setMesa(mesa);
        venta.setEstado(EstadoPedido.PENDIENTE);
        venta.setCobrado(false);
        venta.setMoneda("ARS");
        venta.setMetodoPago(normalizarMetodoPago(null, config));
        venta.setTotalNeto(0.0);
        venta.setTotalIva(0.0);
        venta.setTotalFinal(0.0);
        venta.setItems(new ArrayList<>());
        venta.setNombreContacto(mesa.etiqueta());
        venta.setObservaciones("Cuenta abierta · " + mesa.etiqueta());

        Venta saved = ventaRepository.save(venta);
        saved.setNroComprobante("M-" + saved.getId());
        saved = ventaRepository.save(saved);
        marcarMesaOcupada(mesa);
        cajaService.reconstruirTurno(caja);

        auditoryLogService.registrar("CREATE", "VENTA", saved.getId(),
                "Cuenta abierta en " + mesa.etiqueta() + " (#" + saved.getId() + ")",
                null, detalleVenta(saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public Venta cuentaAbiertaDeMesa(Long mesaId) {
        String tenant = TenantContext.getCurrentTenant();
        mesaRepository.findByIdAndTenantId(mesaId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada: " + mesaId));
        Venta abierta = ventaRepository.findCuentasAbiertasByMesa(tenant, mesaId).stream()
                .findFirst()
                .orElse(null);
        if (abierta == null) {
            return null;
        }
        return obtenerDetalle(abierta.getId());
    }

    @Transactional
    public Venta actualizarCuentaSalon(Long mesaId, VentaDTO dto) {
        Venta abierta = cuentaAbiertaDeMesa(mesaId);
        if (abierta == null) {
            throw new BusinessException("Esa mesa no tiene una cuenta abierta.");
        }
        if (!CanalVenta.esSalon(abierta.getCanal())) {
            throw new BusinessException("Esa cuenta no es de salón.");
        }
        if (dto == null) {
            dto = new VentaDTO();
        }
        if (dto.getItems() == null) {
            dto.setItems(List.of());
        }
        return actualizarPedido(abierta.getId(), dto);
    }

    @Transactional
    public Venta cobrarCerrarMesa(Long mesaId, VentaCobroDTO dto) {
        Venta abierta = cuentaAbiertaDeMesa(mesaId);
        if (abierta == null) {
            throw new BusinessException("Esa mesa no tiene una cuenta abierta.");
        }
        if (abierta.getItems() == null || abierta.getItems().isEmpty()) {
            throw new BusinessException("Cargá al menos un producto antes de cobrar la mesa.");
        }
        double pagado = sumaPartesMonetarias(abierta);
        if (DivisionCuenta.cubreElTotal(nz(abierta.getTotalFinal()), pagado)) {
            Venta cerrada = cerrarCuentaSinReponerStock(abierta, "Cerrada: las partes cubren el total.");
            String tenantCerrada = TenantContext.getCurrentTenant();
            cajaRepository.findByEstadoAndTenantId("ABIERTA", tenantCerrada)
                    .ifPresent(cajaService::reconstruirTurno);
            return obtenerDetalle(cerrada.getId());
        }
        if (pagado > 0) {
            MesaDivisionDTO resto = new MesaDivisionDTO();
            resto.setMonto(DivisionCuenta.saldo(nz(abierta.getTotalFinal()), pagado));
            resto.setMetodoPago(dto != null ? dto.getMetodoPago() : null);
            resto.setMontoAbonado(dto != null ? dto.getMontoAbonado() : null);
            return dividirCuentaSalon(mesaId, resto);
        }
        VentaCobroDTO cobro = dto == null ? new VentaCobroDTO() : dto;
        cobro.setCobrado(true);
        Venta cobrada = actualizarCobro(abierta.getId(), cobro);
        cobrada.setEstado(EstadoPedido.PAGADA);
        Venta saved = ventaRepository.save(cobrada);
        liberarMesaSiCorresponde(saved);
        String tenant = TenantContext.getCurrentTenant();
        cajaRepository.findByEstadoAndTenantId("ABIERTA", tenant)
                .ifPresent(cajaService::reconstruirTurno);
        auditoryLogService.registrar("UPDATE", "VENTA", saved.getId(),
                "Mesa cobrada y cerrada (#" + saved.getId() + ")",
                null, detalleVenta(saved));
        return obtenerDetalle(saved.getId());
    }

    @Transactional
    public Venta pasarCuentaSalon(Long mesaOrigenId, Long mesaDestinoId) {
        if (mesaOrigenId == null || mesaDestinoId == null) {
            throw new BusinessException("Indicá la mesa destino.");
        }
        if (mesaOrigenId.equals(mesaDestinoId)) {
            throw new BusinessException("Elegí otra mesa. No se puede pasar a la misma.");
        }
        Venta cuenta = cuentaAbiertaDeMesa(mesaOrigenId);
        if (cuenta == null) {
            throw new BusinessException("Esa mesa no tiene una cuenta abierta.");
        }
        String tenant = TenantContext.getCurrentTenant();
        Mesa destino = mesaRepository.findByIdAndTenantId(mesaDestinoId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada: " + mesaDestinoId));
        if (!destino.isActiva()) {
            throw new BusinessException("La mesa destino está desactivada.");
        }
        asegurarMesaLibre(destino, cuenta.getId());
        Mesa origen = cuenta.getMesa();
        String detalleAnterior = detalleVenta(cuenta);
        cuenta.setMesa(destino);
        cuenta.setNombreContacto(destino.etiqueta());
        if (cuenta.getObservaciones() != null && cuenta.getObservaciones().startsWith("Cuenta abierta")) {
            cuenta.setObservaciones("Cuenta abierta · " + destino.etiqueta());
        }
        Venta saved = ventaRepository.save(cuenta);
        marcarMesaOcupada(destino);
        if (origen != null && !origen.getId().equals(destino.getId())) {
            origen.setEstado(Mesa.LIBRE);
            mesaRepository.save(origen);
        }
        auditoryLogService.registrar("UPDATE", "VENTA", saved.getId(),
                "Cuenta pasada de " + (origen != null ? origen.etiqueta() : "mesa") + " a " + destino.etiqueta(),
                detalleAnterior, detalleVenta(saved));
        return obtenerDetalle(saved.getId());
    }

    @Transactional
    public Venta juntarCuentasSalon(Long mesaOrigenId, Long mesaDestinoId) {
        if (mesaOrigenId == null || mesaDestinoId == null) {
            throw new BusinessException("Indicá la mesa destino.");
        }
        if (mesaOrigenId.equals(mesaDestinoId)) {
            throw new BusinessException("Elegí otra mesa. No se puede juntar consigo misma.");
        }
        Venta origen = cuentaAbiertaDeMesa(mesaOrigenId);
        Venta destino = cuentaAbiertaDeMesa(mesaDestinoId);
        if (origen == null) {
            throw new BusinessException("La mesa origen no tiene una cuenta abierta.");
        }
        if (destino == null) {
            throw new BusinessException("La mesa destino no tiene cuenta. Usá pasar de mesa.");
        }
        if (!CanalVenta.esSalon(origen.getCanal()) || !CanalVenta.esSalon(destino.getCanal())) {
            throw new BusinessException("Solo se juntan cuentas de salón.");
        }
        if (origen.getItems() == null || origen.getItems().isEmpty()) {
            throw new BusinessException("La mesa origen no tiene platos. Usá pasar de mesa si solo cambian de lugar.");
        }
        Mesa mesaOrigen = origen.getMesa();
        Mesa mesaDestino = destino.getMesa();
        String etiquetaOrigen = mesaOrigen != null ? mesaOrigen.etiqueta() : "mesa";
        String etiquetaDestino = mesaDestino != null ? mesaDestino.etiqueta() : "mesa";
        String detalleAnterior = detalleVenta(destino) + " | origen " + detalleVenta(origen);
        String tenant = TenantContext.getCurrentTenant();

        if (destino.getItems() == null) {
            destino.setItems(new ArrayList<>());
        }
        for (ItemVenta item : origen.getItems()) {
            ItemVenta copia = new ItemVenta();
            copia.setVenta(destino);
            copia.setProducto(item.getProducto());
            copia.setCantidad(item.getCantidad());
            copia.setPrecioUnitario(item.getPrecioUnitario());
            copia.setTenantId(tenant);
            String nota = textoOpcional(item.getObservaciones());
            String marca = "de " + etiquetaOrigen;
            copia.setObservaciones(nota == null ? marca : nota + " · " + marca);
            destino.getItems().add(copia);
        }
        recargarTotales(destino);
        if (EstadoPedido.LISTO.equals(destino.getEstado()) || EstadoPedido.EN_CAMINO.equals(destino.getEstado())) {
            destino.setEstado(EstadoPedido.PENDIENTE);
        }
        String extra = "Incluye " + etiquetaOrigen + ".";
        String obs = textoOpcional(destino.getObservaciones());
        if (obs == null || !obs.contains(extra)) {
            destino.setObservaciones(obs == null ? extra : obs + " " + extra);
        }
        Venta savedDestino = ventaRepository.save(destino);

        if (origen.getItems() != null) {
            origen.getItems().clear();
        }
        origen.setTotalNeto(0.0);
        origen.setTotalIva(0.0);
        origen.setTotalFinal(0.0);
        origen.setEstado(EstadoPedido.ANULADA);
        origen.setObservaciones("Juntada con " + etiquetaDestino);
        Venta savedOrigen = ventaRepository.save(origen);
        liberarMesaSiCorresponde(savedOrigen);

        auditoryLogService.registrar("UPDATE", "VENTA", savedDestino.getId(),
                "Cuentas juntadas: " + etiquetaOrigen + " → " + etiquetaDestino,
                detalleAnterior, detalleVenta(savedDestino));
        return obtenerDetalle(savedDestino.getId());
    }

    @Transactional
    public Venta dividirCuentaSalon(Long mesaId, MesaDivisionDTO dto) {
        Venta origen = cuentaAbiertaDeMesa(mesaId);
        if (origen == null) {
            throw new BusinessException("Esa mesa no tiene una cuenta abierta.");
        }
        if (!CanalVenta.esSalon(origen.getCanal())) {
            throw new BusinessException("Solo se divide una cuenta de salón.");
        }
        Caja caja = cajaService.exigirCajaOperativa();
        String tenant = TenantContext.getCurrentTenant();
        TenantConfig config = tenantConfigRepository.findByTenantId(tenant).orElse(null);
        String metodo = normalizarMetodoPago(dto != null ? dto.getMetodoPago() : null, config);
        boolean hayPlatos = dto != null && dto.getItems() != null
                && dto.getItems().stream().anyMatch(i -> i.getCantidad() != null && i.getCantidad() > 0);

        Venta parte;
        if (hayPlatos) {
            if (!partesMonetarias(origen.getId()).isEmpty()) {
                throw new BusinessException("Esta mesa ya tiene partes en dinero. Cobrá el saldo o seguí en partes iguales.");
            }
            parte = cobrarPartePorPlatos(origen, dto, metodo);
        } else if (dto != null && dto.getPartes() != null) {
            parte = cobrarParteIgual(origen, dto, metodo);
        } else if (dto != null && dto.getMonto() != null) {
            parte = cobrarParteMonto(origen, dto, metodo);
        } else {
            throw new BusinessException("Elegí platos, un número de partes, o un monto.");
        }
        cajaService.reconstruirTurno(caja);
        return obtenerDetalle(parte.getId());
    }

    @Transactional
    public Venta actualizarCobro(Long id, VentaCobroDTO dto) {
        if (dto == null || dto.getCobrado() == null) {
            throw new BusinessException("Indicá si el pedido está cobrado o no.");
        }
        String tenant = TenantContext.getCurrentTenant();
        Venta venta = ventaRepository.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con id: " + id));
        if (EstadoPedido.ANULADA.equals(venta.getEstado())) {
            throw new BusinessException("No se puede cobrar un pedido anulado.");
        }
        Usuario actor = usuarioActualOpcional();
        restringirCobroSiRepartidor(actor, venta);
        String detalleAnterior = detalleVenta(venta);
        boolean cobrado = Boolean.TRUE.equals(dto.getCobrado());
        venta.setCobrado(cobrado);
        if (cobrado) {
            TenantConfig config = tenantConfigRepository.findByTenantId(tenant).orElse(null);
            if (dto.getMetodoPago() != null && !dto.getMetodoPago().isBlank()) {
                venta.setMetodoPago(normalizarMetodoPago(dto.getMetodoPago(), config));
            }
            if (EFECTIVO.equals(venta.getMetodoPago())) {
                double total = venta.getTotalFinal() != null ? venta.getTotalFinal() : 0;
                double abonado = dto.getMontoAbonado() != null ? dto.getMontoAbonado() : total;
                venta.setMontoAbonado(abonado);
                venta.setVuelto(Math.max(0, abonado - total));
            }
        }
        Venta saved = ventaRepository.save(venta);
        cajaRepository.findByEstadoAndTenantId("ABIERTA", tenant)
                .ifPresent(cajaService::reconstruirTurno);
        auditoryLogService.registrar("UPDATE", "VENTA", saved.getId(),
                "Pedido #" + saved.getId() + (cobrado ? " marcado cobrado" : " marcado sin cobrar"),
                detalleAnterior, detalleVenta(saved));
        return saved;
    }

    @Transactional
    public Venta actualizarPedido(Long id, VentaDTO dto) {
        if (dto == null || dto.getItems() == null) {
            throw new BusinessException("El pedido necesita al menos un producto.");
        }
        String tenant = TenantContext.getCurrentTenant();
        Venta venta = ventaRepository.findDetalleByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con id: " + id));
        if (!EstadoPedido.esActivo(venta.getEstado())) {
            throw new BusinessException("Solo se pueden editar pedidos activos de cocina.");
        }
        boolean salon = CanalVenta.esSalon(venta.getCanal());
        if (!CanalVenta.esPedido(venta.getCanal()) && !salon) {
            throw new BusinessException("Solo se editan pedidos de WhatsApp, delivery, retiro o cuenta de mesa.");
        }
        if (dto.getItems().isEmpty() && !salon) {
            throw new BusinessException("El pedido necesita al menos un producto.");
        }

        String detalleAnterior = detalleVenta(venta);
        Map<Long, Integer> cantidadAnterior = new HashMap<>();
        if (venta.getItems() != null) {
            for (ItemVenta item : venta.getItems()) {
                if (item.getProducto() != null) {
                    cantidadAnterior.merge(item.getProducto().getId(), item.getCantidad(), Integer::sum);
                }
            }
        }

        TenantConfig config = tenantConfigRepository.findByTenantId(tenant).orElse(null);
        if (dto.getObservaciones() != null) {
            venta.setObservaciones(textoOpcional(dto.getObservaciones()));
        }
        if (dto.getNombreContacto() != null) {
            venta.setNombreContacto(textoOpcional(dto.getNombreContacto()));
        }
        if (dto.getTelefonoContacto() != null) {
            venta.setTelefonoContacto(textoOpcional(dto.getTelefonoContacto()));
        }
        if (dto.getDireccionEntrega() != null) {
            venta.setDireccionEntrega(textoOpcional(dto.getDireccionEntrega()));
        }
        CanalVenta.exigirDireccionSiDelivery(venta.getCanal(), venta.getDireccionEntrega());
        if (dto.getMetodoPago() != null && !dto.getMetodoPago().isBlank()) {
            venta.setMetodoPago(normalizarMetodoPago(dto.getMetodoPago(), config));
        }

        List<ItemVenta> nuevosItems = new ArrayList<>();
        Map<Long, Integer> cantidadNueva = new HashMap<>();
        double subtotal = 0;

        for (ItemVentaDTO itemDto : dto.getItems()) {
            Producto p = productoRepository.findByIdAndTenantId(itemDto.getProductoId(), tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + itemDto.getProductoId()));
            if (!p.isVendible()) {
                throw new BusinessException(p.getNombre() + " es de depósito. No se vende: usalo en una receta o marcalo como vendible.");
            }
            int cantidad = itemDto.getCantidad() != null ? itemDto.getCantidad() : 0;
            if (cantidad <= 0) {
                throw new BusinessException("La cantidad vendida debe ser mayor a cero para: " + p.getNombre());
            }
            cantidadNueva.merge(p.getId(), cantidad, Integer::sum);

            ItemVenta item = new ItemVenta();
            item.setVenta(venta);
            item.setProducto(p);
            item.setCantidad(cantidad);
            item.setPrecioUnitario(p.getPrecio());
            item.setObservaciones(textoOpcional(itemDto.getObservaciones()));
            item.setTenantId(tenant);
            nuevosItems.add(item);
            subtotal += p.getPrecio() * cantidad;
        }

        Usuario usuario = resolverUsuarioMovimiento(tenant);
        Set<Long> productos = new HashSet<>();
        productos.addAll(cantidadAnterior.keySet());
        productos.addAll(cantidadNueva.keySet());
        for (Long productoId : productos) {
            int antes = cantidadAnterior.getOrDefault(productoId, 0);
            int despues = cantidadNueva.getOrDefault(productoId, 0);
            int delta = despues - antes;
            if (delta == 0) {
                continue;
            }
            Producto producto = productoRepository.findByIdAndTenantId(productoId, tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
            if (delta > 0) {
                recetaService.aplicarVenta(producto, delta, usuario, venta, false);
            } else {
                recetaService.aplicarVenta(producto, Math.abs(delta), usuario, venta, true);
            }
        }

        if (venta.getItems() == null) {
            venta.setItems(new ArrayList<>());
        } else {
            venta.getItems().clear();
        }
        venta.getItems().addAll(nuevosItems);

        double ivaPorcentaje = ivaPorcentaje(config);
        double totalIva = subtotal * ivaPorcentaje / 100.0;
        double totalFinal = subtotal + totalIva;
        venta.setTotalNeto(subtotal);
        venta.setTotalIva(totalIva);
        venta.setTotalFinal(totalFinal);
        if (salon) {
            boolean hayNuevaComanda = false;
            for (Long productoId : productos) {
                if (cantidadNueva.getOrDefault(productoId, 0) > cantidadAnterior.getOrDefault(productoId, 0)) {
                    hayNuevaComanda = true;
                    break;
                }
            }
            if (hayNuevaComanda && (EstadoPedido.LISTO.equals(venta.getEstado())
                    || EstadoPedido.EN_CAMINO.equals(venta.getEstado()))) {
                venta.setEstado(EstadoPedido.PENDIENTE);
            }
        }
        if (EFECTIVO.equals(venta.getMetodoPago()) && venta.isCobrado()) {
            double abonado = dto.getMontoAbonado() != null ? dto.getMontoAbonado() : totalFinal;
            venta.setMontoAbonado(abonado);
            venta.setVuelto(Math.max(0, abonado - totalFinal));
        }

        Venta saved = ventaRepository.save(venta);
        cajaRepository.findByEstadoAndTenantId("ABIERTA", tenant)
                .ifPresent(cajaService::reconstruirTurno);
        auditoryLogService.registrar("UPDATE", "VENTA", saved.getId(),
                "Pedido #" + saved.getId() + " editado",
                detalleAnterior, detalleVenta(saved));
        return saved;
    }

    @Transactional
    public Venta anularVenta(Long id) {
        String tenant = TenantContext.getCurrentTenant();

        Venta venta = ventaRepository.findDetalleByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con id: " + id));

        if ("ANULADA".equals(venta.getEstado())) {
            throw new BusinessException("La venta ya fue anulada anteriormente.");
        }

        Usuario usuario = stockMovementService.getCurrentUser();
        String detalleAnterior = detalleVenta(venta);

        if (venta.getItems() != null) {
            for (ItemVenta item : venta.getItems()) {
                if (item.getProducto() == null || item.getCantidad() == null || item.getCantidad() <= 0) {
                    continue;
                }
                recetaService.aplicarVenta(item.getProducto(), item.getCantidad(), usuario, venta, true);
            }
        }

        venta.setEstado("ANULADA");
        Venta saved = ventaRepository.save(venta);
        liberarMesaSiCorresponde(saved);
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
            aplicarFiltroRepartidor(predicates, cb, root);
            return cb.and(predicates.toArray(new Predicate[0]));
        }, ordenado);
        Map<Long, Venta> conItems = hidratarItems(page.getContent());
        return page.map(venta -> toListado(conItems.getOrDefault(venta.getId(), venta)));
    }

    public List<VentaListadoDTO> getAllVentas(String tenantId) {
        return buscarVentas(tenantId, null, null, null, null, null, false,
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "fecha"))).getContent();
    }

    @Transactional(readOnly = true)
    public Venta obtenerDetalle(Long id) {
        String tenant = TenantContext.getCurrentTenant();
        Venta venta = ventaRepository.findDetalleByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con id: " + id));
        Usuario actor = usuarioActualOpcional();
        if (actor != null && actor.getRol() == Rol.REPARTIDOR
                && !PedidoSalida.visibleParaRepartidor(
                        venta.getEstado(), venta.getCanal(), venta.getDireccionEntrega(),
                        venta.getRepartidorUsuarioId(), actor.getId())) {
            throw new ResourceNotFoundException("Venta no encontrada con id: " + id);
        }
        return venta;
    }

    @Transactional(readOnly = true)
    public SalidaPedidosDTO obtenerSalida() {
        String tenant = TenantContext.getCurrentTenant();
        Usuario actor = exigirUsuarioLogin();
        List<Venta> listosCrudos = ventaRepository.findByTenantIdAndEstadoOrderByFechaAsc(tenant, EstadoPedido.LISTO)
                .stream()
                .filter(v -> PedidoSalida.puedeTomar(
                        v.getEstado(), v.getCanal(), v.getDireccionEntrega(), v.getRepartidorUsuarioId()))
                .toList();
        List<Venta> caminoCrudos = actor.getRol() == Rol.REPARTIDOR
                ? ventaRepository.findByTenantIdAndEstadoAndRepartidorUsuarioIdOrderByFechaAsc(
                        tenant, EstadoPedido.EN_CAMINO, actor.getId())
                : ventaRepository.findByTenantIdAndEstadoOrderByFechaAsc(tenant, EstadoPedido.EN_CAMINO);
        Map<Long, Venta> hidratados = hidratarItems(concatenar(listosCrudos, caminoCrudos));
        List<VentaListadoDTO> listos = listosCrudos.stream()
                .map(v -> toListado(hidratados.getOrDefault(v.getId(), v)))
                .toList();
        List<VentaListadoDTO> camino = caminoCrudos.stream()
                .map(v -> toListado(hidratados.getOrDefault(v.getId(), v)))
                .toList();
        return new SalidaPedidosDTO(listos, camino);
    }

    public VentaListadoDTO toListado(Venta venta) {
        VentaListadoDTO dto = VentaListadoDTO.desde(venta);
        Usuario actor = usuarioActualOpcional();
        dto.setPuedeTomar(PedidoSalida.puedeTomar(
                venta.getEstado(), venta.getCanal(), venta.getDireccionEntrega(), venta.getRepartidorUsuarioId()));
        boolean enCamino = EstadoPedido.EN_CAMINO.equals(venta.getEstado());
        if (!enCamino || actor == null) {
            dto.setPuedeLiberar(false);
        } else if (actor.getRol() == Rol.REPARTIDOR) {
            dto.setPuedeLiberar(actor.getId().equals(venta.getRepartidorUsuarioId()));
        } else {
            dto.setPuedeLiberar(true);
        }
        return dto;
    }

    public VentaListadoDTO toListadoConSaldo(Venta venta) {
        VentaListadoDTO dto = toListado(venta);
        if (venta == null || venta.getId() == null) {
            return dto;
        }
        double pagado = sumaPartesMonetarias(venta);
        dto.setMontoPagado(pagado);
        dto.setSaldo(DivisionCuenta.saldo(nz(venta.getTotalFinal()), pagado));
        return dto;
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

    public long contarVentasNoAnuladasHoy(String tenantId) {
        LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
        return ventaRepository.findByTenantIdAndFechaAfter(tenantId, inicioHoy).stream()
                .filter(v -> !"ANULADA".equals(v.getEstado()))
                .count();
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

    private void recargarTotales(Venta venta) {
        double subtotal = 0;
        if (venta.getItems() != null) {
            for (ItemVenta item : venta.getItems()) {
                double precio = item.getPrecioUnitario() != null ? item.getPrecioUnitario() : 0;
                int cantidad = item.getCantidad() != null ? item.getCantidad() : 0;
                subtotal += precio * cantidad;
            }
        }
        TenantConfig config = tenantConfigRepository.findByTenantId(TenantContext.getCurrentTenant()).orElse(null);
        double iva = subtotal * ivaPorcentaje(config) / 100.0;
        venta.setTotalNeto(subtotal);
        venta.setTotalIva(iva);
        venta.setTotalFinal(subtotal + iva);
    }

    private Venta cobrarPartePorPlatos(Venta origen, MesaDivisionDTO dto, String metodo) {
        List<ItemVenta> extraidos = extraerItemsSinTocarStock(origen, dto.getItems());
        if (extraidos.isEmpty()) {
            throw new BusinessException("Elegí al menos un plato para cobrar.");
        }
        String etiqueta = origen.getMesa() != null ? origen.getMesa().etiqueta() : "mesa";
        Venta parte = nuevaParte(origen, metodo, "Parte de " + etiqueta);
        for (ItemVenta item : extraidos) {
            item.setVenta(parte);
            parte.getItems().add(item);
        }
        recargarTotales(parte);
        aplicarAbono(parte, dto.getMontoAbonado());
        Venta savedParte = persistirParte(parte);
        recargarTotales(origen);
        if (origen.getItems() == null || origen.getItems().isEmpty()) {
            cerrarCuentaSinReponerStock(origen, "Cerrada al cobrar la última parte (" + savedParte.getNroComprobante() + ").");
        } else {
            ventaRepository.save(origen);
        }
        auditoryLogService.registrar("CREATE", "VENTA", savedParte.getId(),
                "Parte de mesa cobrada (#" + savedParte.getId() + ")",
                detalleVenta(origen), detalleVenta(savedParte));
        return savedParte;
    }

    private Venta cobrarParteIgual(Venta origen, MesaDivisionDTO dto, String metodo) {
        List<Venta> cobradas = partesMonetarias(origen.getId());
        double pagado = sumaTotales(cobradas);
        double monto = DivisionCuenta.parteActual(nz(origen.getTotalFinal()), pagado, dto.getPartes(), cobradas.size());
        return cobrarParteDinero(origen, monto, metodo, dto.getMontoAbonado(),
                "Parte " + (cobradas.size() + 1) + "/" + dto.getPartes());
    }

    private Venta cobrarParteMonto(Venta origen, MesaDivisionDTO dto, String metodo) {
        double pagado = sumaPartesMonetarias(origen);
        double saldo = DivisionCuenta.saldo(nz(origen.getTotalFinal()), pagado);
        double monto = DivisionCuenta.cobrarMonto(saldo, dto.getMonto());
        return cobrarParteDinero(origen, monto, metodo, dto.getMontoAbonado(), "Parte de cuenta");
    }

    private Venta cobrarParteDinero(Venta origen, double monto, String metodo, Double montoAbonado, String nota) {
        String etiqueta = origen.getMesa() != null ? origen.getMesa().etiqueta() : "mesa";
        Venta parte = nuevaParte(origen, metodo, nota + " · " + etiqueta);
        aplicarTotalesMonto(parte, origen, monto);
        aplicarAbono(parte, montoAbonado);
        Venta savedParte = persistirParte(parte);
        double pagado = sumaPartesMonetarias(origen);
        if (DivisionCuenta.cubreElTotal(nz(origen.getTotalFinal()), pagado)) {
            cerrarCuentaSinReponerStock(origen, "Cerrada: las partes cubren el total.");
        }
        auditoryLogService.registrar("CREATE", "VENTA", savedParte.getId(),
                "Parte de mesa cobrada (#" + savedParte.getId() + ")",
                detalleVenta(origen), detalleVenta(savedParte));
        return savedParte;
    }

    private Venta nuevaParte(Venta origen, String metodo, String observaciones) {
        String tenant = TenantContext.getCurrentTenant();
        Venta parte = new Venta();
        parte.setTenantId(tenant);
        parte.setCanal(CanalVenta.SALON);
        parte.setMesa(origen.getMesa());
        parte.setEstado(EstadoPedido.PAGADA);
        parte.setCobrado(true);
        parte.setMoneda(origen.getMoneda() != null ? origen.getMoneda() : "ARS");
        parte.setMetodoPago(metodo);
        parte.setVentaOrigenId(origen.getId());
        parte.setNombreContacto(origen.getNombreContacto());
        parte.setCliente(origen.getCliente());
        parte.setObservaciones(observaciones);
        parte.setItems(new ArrayList<>());
        parte.setTotalNeto(0.0);
        parte.setTotalIva(0.0);
        parte.setTotalFinal(0.0);
        return parte;
    }

    private Venta persistirParte(Venta parte) {
        Venta saved = ventaRepository.save(parte);
        saved.setNroComprobante("M-" + saved.getId());
        return ventaRepository.save(saved);
    }

    private void aplicarAbono(Venta parte, Double montoAbonado) {
        if (!EFECTIVO.equals(parte.getMetodoPago())) {
            return;
        }
        double total = nz(parte.getTotalFinal());
        double abonado = montoAbonado != null ? montoAbonado : total;
        parte.setMontoAbonado(abonado);
        parte.setVuelto(Math.max(0, abonado - total));
    }

    private void aplicarTotalesMonto(Venta parte, Venta origen, double monto) {
        double origenTotal = nz(origen.getTotalFinal());
        double ratio = origenTotal > 0 ? monto / origenTotal : 1;
        parte.setTotalFinal(DivisionCuenta.redondear(monto));
        parte.setTotalNeto(DivisionCuenta.redondear(nz(origen.getTotalNeto()) * ratio));
        parte.setTotalIva(DivisionCuenta.redondear(parte.getTotalFinal() - parte.getTotalNeto()));
    }

    private List<ItemVenta> extraerItemsSinTocarStock(Venta origen, List<ItemVentaDTO> pedidos) {
        if (origen.getItems() == null) {
            origen.setItems(new ArrayList<>());
        }
        Map<Long, Integer> pedir = new HashMap<>();
        if (pedidos != null) {
            for (ItemVentaDTO dto : pedidos) {
                if (dto.getProductoId() == null || dto.getCantidad() == null || dto.getCantidad() <= 0) {
                    continue;
                }
                pedir.merge(dto.getProductoId(), dto.getCantidad(), Integer::sum);
            }
        }
        if (pedir.isEmpty()) {
            return List.of();
        }
        String tenant = TenantContext.getCurrentTenant();
        List<ItemVenta> extraidos = new ArrayList<>();
        for (Map.Entry<Long, Integer> pedido : pedir.entrySet()) {
            int falta = pedido.getValue();
            int disponible = 0;
            for (ItemVenta item : origen.getItems()) {
                if (item.getProducto() != null && pedido.getKey().equals(item.getProducto().getId())) {
                    disponible += item.getCantidad() != null ? item.getCantidad() : 0;
                }
            }
            if (falta > disponible) {
                throw new BusinessException("No hay tantos platos en la mesa para cobrar esa parte.");
            }
            Iterator<ItemVenta> it = origen.getItems().iterator();
            while (falta > 0 && it.hasNext()) {
                ItemVenta item = it.next();
                if (item.getProducto() == null || !pedido.getKey().equals(item.getProducto().getId())) {
                    continue;
                }
                int qty = item.getCantidad() != null ? item.getCantidad() : 0;
                int tomar = Math.min(qty, falta);
                ItemVenta copia = new ItemVenta();
                copia.setProducto(item.getProducto());
                copia.setCantidad(tomar);
                copia.setPrecioUnitario(item.getPrecioUnitario());
                copia.setObservaciones(item.getObservaciones());
                copia.setTenantId(tenant);
                extraidos.add(copia);
                falta -= tomar;
                if (tomar >= qty) {
                    it.remove();
                } else {
                    item.setCantidad(qty - tomar);
                }
            }
        }
        return extraidos;
    }

    private Venta cerrarCuentaSinReponerStock(Venta venta, String observaciones) {
        if (venta.getItems() != null) {
            venta.getItems().clear();
        }
        venta.setTotalNeto(0.0);
        venta.setTotalIva(0.0);
        venta.setTotalFinal(0.0);
        venta.setEstado(EstadoPedido.ANULADA);
        if (observaciones != null) {
            venta.setObservaciones(observaciones);
        }
        Venta saved = ventaRepository.save(venta);
        liberarMesaSiCorresponde(saved);
        return saved;
    }

    private double sumaPartesMonetarias(Venta origen) {
        if (origen == null || origen.getId() == null) {
            return 0;
        }
        return sumaTotales(partesMonetarias(origen.getId()));
    }

    private List<Venta> partesMonetarias(Long origenId) {
        return ventaRepository.findByTenantIdAndVentaOrigenIdOrderByFechaAsc(
                        TenantContext.getCurrentTenant(), origenId)
                .stream()
                .filter(v -> v.isCobrado() && !EstadoPedido.ANULADA.equals(v.getEstado()))
                .filter(v -> v.getItems() == null || v.getItems().isEmpty())
                .toList();
    }

    private double sumaTotales(List<Venta> ventas) {
        return DivisionCuenta.redondear(ventas.stream()
                .mapToDouble(v -> nz(v.getTotalFinal()))
                .sum());
    }

    private double nz(Double valor) {
        return valor != null ? valor : 0;
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
                "cobrado", venta.isCobrado(),
                "totalFinal", venta.getTotalFinal(),
                "montoAbonado", venta.getMontoAbonado(),
                "vuelto", venta.getVuelto(),
                "repartidor", venta.getRepartidorNombre(),
                "items", venta.getItems() != null ? venta.getItems().size() : 0
        );
    }

    private void aplicarFiltroCanal(List<Predicate> predicates, jakarta.persistence.criteria.CriteriaBuilder cb,
                                    jakarta.persistence.criteria.Root<Venta> root,
                                    String canal, boolean soloPedidos, boolean soloWhatsapp) {
        if (canal != null && !canal.isBlank()) {
            String normalizado = CanalVenta.normalizar(canal);
            predicates.add(cb.equal(root.get("canal"), normalizado));
            if (CanalVenta.esSalon(normalizado)) {
                predicates.add(cb.isNotEmpty(root.get("items")));
            }
            return;
        }
        if (soloPedidos) {
            predicates.add(root.get("canal").in(CanalVenta.canalesCocina()));
            predicates.add(cb.or(
                    cb.notEqual(root.get("canal"), CanalVenta.SALON),
                    cb.isNotEmpty(root.get("items"))
            ));
            return;
        }
        if (soloWhatsapp) {
            predicates.add(cb.equal(root.get("canal"), CanalVenta.WHATSAPP));
        }
    }

    private void aplicarFiltroRepartidor(List<Predicate> predicates, CriteriaBuilder cb, Root<Venta> root) {
        Usuario actor = usuarioActualOpcional();
        if (actor == null || actor.getRol() != Rol.REPARTIDOR) {
            return;
        }
        Predicate listos = cb.and(
                cb.equal(root.get("estado"), EstadoPedido.LISTO),
                cb.isNull(root.get("repartidorUsuarioId")),
                predicadoEnvio(cb, root)
        );
        Predicate mios = cb.and(
                cb.equal(root.get("repartidorUsuarioId"), actor.getId()),
                cb.equal(root.get("estado"), EstadoPedido.EN_CAMINO)
        );
        predicates.add(cb.or(listos, mios));
    }

    private Predicate predicadoEnvio(CriteriaBuilder cb, Root<Venta> root) {
        Predicate delivery = cb.equal(root.get("canal"), CanalVenta.DELIVERY);
        Predicate whatsappConDir = cb.and(
                cb.equal(root.get("canal"), CanalVenta.WHATSAPP),
                cb.isNotNull(root.get("direccionEntrega")),
                cb.notEqual(cb.trim(root.get("direccionEntrega")), "")
        );
        return cb.or(delivery, whatsappConDir);
    }

    private void restringirEstadoSiRepartidor(Usuario actor, Venta venta, String destino) {
        if (actor == null || actor.getRol() != Rol.REPARTIDOR) {
            return;
        }
        if (!EstadoPedido.ENTREGADO.equals(destino)) {
            throw new BusinessException("El cadete solo puede marcar el pedido como entregado.");
        }
        if (venta.getRepartidorUsuarioId() == null || !venta.getRepartidorUsuarioId().equals(actor.getId())) {
            throw new BusinessException("Este envío no es tuyo.");
        }
    }

    private void restringirCobroSiRepartidor(Usuario actor, Venta venta) {
        if (actor == null || actor.getRol() != Rol.REPARTIDOR) {
            return;
        }
        if (venta.getRepartidorUsuarioId() == null || !venta.getRepartidorUsuarioId().equals(actor.getId())) {
            throw new BusinessException("Este envío no es tuyo.");
        }
    }

    private Usuario usuarioActualOpcional() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return null;
            }
            return auditoryLogService.getCurrentUser();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Usuario exigirUsuarioLogin() {
        Usuario usuario = usuarioActualOpcional();
        if (usuario == null) {
            throw new BusinessException("Necesitás iniciar sesión.");
        }
        return usuario;
    }

    public Usuario cadetePorTelefono(String telefonoRaw) {
        String tenant = TenantContext.getCurrentTenant();
        String telefono = TelefonoWhatsApp.normalizar(telefonoRaw);
        if (telefono == null) {
            throw new BusinessException("WhatsApp de cadete inválido.");
        }
        return usuarioRepository.findAllByTenantIdAndRol(tenant, Rol.REPARTIDOR).stream()
                .filter(u -> TelefonoWhatsApp.mismaLinea(telefono, u.getTelefono()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Ese número no está vinculado como cadete de este comercio."));
    }

    public List<BotCadeteDTO> listarCadetesWhatsApp() {
        String tenant = TenantContext.getCurrentTenant();
        return usuarioRepository.findAllByTenantIdAndRol(tenant, Rol.REPARTIDOR).stream()
                .filter(u -> TelefonoWhatsApp.normalizar(u.getTelefono()) != null)
                .map(u -> new BotCadeteDTO(
                        u.getId(),
                        PedidoSalida.nombreEquipo(u),
                        TelefonoWhatsApp.normalizar(u.getTelefono()),
                        u.getEmail(),
                        "DELIVERY"))
                .toList();
    }

    public List<BotCadeteDTO> listarDuenosWhatsApp() {
        return listarStaffWhatsApp(Rol.ADMIN, "SOCIO");
    }

    public List<BotCadeteDTO> listarCajasWhatsApp() {
        return listarStaffWhatsApp(Rol.OPERADOR, "CAJA");
    }

    public BotEquipoDTO listarEquipoWhatsApp() {
        BotEquipoDTO dto = new BotEquipoDTO();
        dto.setSocios(listarDuenosWhatsApp());
        dto.setCajas(listarCajasWhatsApp());
        dto.setDelivery(listarCadetesWhatsApp());
        return dto;
    }

    private List<BotCadeteDTO> listarStaffWhatsApp(Rol rol, String rolBot) {
        String tenant = TenantContext.getCurrentTenant();
        return usuarioRepository.findAllByTenantIdAndRol(tenant, rol).stream()
                .filter(u -> TelefonoWhatsApp.normalizar(u.getTelefono()) != null)
                .map(u -> new BotCadeteDTO(
                        u.getId(),
                        PedidoSalida.nombreEquipo(u),
                        TelefonoWhatsApp.normalizar(u.getTelefono()),
                        u.getEmail(),
                        rolBot))
                .toList();
    }

    /**
     * Socio (ADMIN) o Caja (OPERADOR) por WhatsApp. Delivery no consulta operación.
     */
    public String rolOperacionBot(String telefonoRaw) {
        Usuario u = staffPorTelefono(telefonoRaw);
        if (u.getRol() == Rol.ADMIN) {
            return "SOCIO";
        }
        if (u.getRol() == Rol.OPERADOR) {
            return "CAJA";
        }
        throw new BusinessException("Ese número es de delivery. Las consultas de salón las ve el Socio o Caja.");
    }

    public Usuario staffPorTelefono(String telefonoRaw) {
        String tenant = TenantContext.getCurrentTenant();
        String telefono = TelefonoWhatsApp.normalizar(telefonoRaw);
        if (telefono == null) {
            throw new BusinessException("WhatsApp inválido.");
        }
        return usuarioRepository.findAllByTenantId(tenant).stream()
                .filter(u -> TelefonoWhatsApp.mismaLinea(telefono, u.getTelefono()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Ese número no está en el equipo. Cargalo en Equipo con WhatsApp."));
    }

    @Transactional(readOnly = true)
    public BotEnviosDTO obtenerEnviosParaBot() {
        String tenant = TenantContext.getCurrentTenant();
        List<Venta> cocina = concatenar(
                ventaRepository.findByTenantIdAndEstadoOrderByFechaAsc(tenant, EstadoPedido.PENDIENTE),
                ventaRepository.findByTenantIdAndEstadoOrderByFechaAsc(tenant, EstadoPedido.EN_PREPARACION)
        ).stream()
                .filter(v -> CanalVenta.esEnvio(v.getCanal(), v.getDireccionEntrega()))
                .toList();
        List<Venta> listos = ventaRepository.findByTenantIdAndEstadoOrderByFechaAsc(tenant, EstadoPedido.LISTO)
                .stream()
                .filter(v -> CanalVenta.esEnvio(v.getCanal(), v.getDireccionEntrega()))
                .toList();
        List<Venta> camino = ventaRepository.findByTenantIdAndEstadoOrderByFechaAsc(tenant, EstadoPedido.EN_CAMINO);
        Map<Long, Venta> hidratados = hidratarItems(concatenar(concatenar(cocina, listos), camino));
        BotEnviosDTO dto = new BotEnviosDTO();
        dto.setEnCocina(mapearListado(cocina, hidratados));
        dto.setListos(mapearListado(listos, hidratados));
        dto.setEnCamino(mapearListado(camino, hidratados));
        List<Venta> paraClientes = concatenar(
                concatenar(
                        ventaRepository.findByTenantIdAndEstadoOrderByFechaAsc(tenant, EstadoPedido.PENDIENTE),
                        ventaRepository.findByTenantIdAndEstadoOrderByFechaAsc(tenant, EstadoPedido.EN_PREPARACION)
                ),
                concatenar(
                        ventaRepository.findByTenantIdAndEstadoOrderByFechaAsc(tenant, EstadoPedido.LISTO),
                        camino
                )
        ).stream()
                .filter(v -> CanalVenta.esPedido(v.getCanal()))
                .filter(v -> textoOpcional(v.getTelefonoContacto()) != null)
                .collect(Collectors.toMap(Venta::getId, v -> v, (a, b) -> a, LinkedHashMap::new))
                .values()
                .stream()
                .toList();
        dto.setParaClientes(mapearListado(paraClientes, hidratarItems(paraClientes)));
        return dto;
    }

    /** Cocina de todo el local: WhatsApp, delivery, retiro y salón. */
    @Transactional(readOnly = true)
    public List<VentaListadoDTO> obtenerCocinaLocalParaBot() {
        String tenant = TenantContext.getCurrentTenant();
        List<Venta> fuego = concatenar(
                ventaRepository.findByTenantIdAndEstadoOrderByFechaAsc(tenant, EstadoPedido.PENDIENTE),
                ventaRepository.findByTenantIdAndEstadoOrderByFechaAsc(tenant, EstadoPedido.EN_PREPARACION)
        ).stream()
                .filter(v -> CanalVenta.esCuentaAbierta(v.getCanal()))
                .toList();
        return mapearListado(fuego, hidratarItems(fuego));
    }

    @Transactional(readOnly = true)
    public List<VentaListadoDTO> obtenerRetirosListosParaBot() {
        String tenant = TenantContext.getCurrentTenant();
        List<Venta> listos = ventaRepository.findByTenantIdAndEstadoOrderByFechaAsc(tenant, EstadoPedido.LISTO)
                .stream()
                .filter(v -> !CanalVenta.esEnvio(v.getCanal(), v.getDireccionEntrega()))
                .filter(v -> CanalVenta.esPedido(v.getCanal()) || CanalVenta.esSalon(v.getCanal()))
                .toList();
        return mapearListado(listos, hidratarItems(listos));
    }

    public Venta tomarSiguienteOId(Long ventaId, Usuario cadete) {
        if (ventaId != null) {
            return tomarPedidoComo(ventaId, cadete);
        }
        String tenant = TenantContext.getCurrentTenant();
        List<Venta> listos = ventaRepository.findByTenantIdAndEstadoOrderByFechaAsc(tenant, EstadoPedido.LISTO)
                .stream()
                .filter(v -> CanalVenta.esEnvio(v.getCanal(), v.getDireccionEntrega()))
                .toList();
        Venta propio = listos.stream()
                .filter(v -> cadete.getId().equals(v.getRepartidorUsuarioId()))
                .findFirst()
                .orElse(null);
        if (propio != null) {
            return tomarPedidoComo(propio.getId(), cadete);
        }
        Venta primero = listos.stream()
                .filter(v -> PedidoSalida.puedeTomar(
                        v.getEstado(), v.getCanal(), v.getDireccionEntrega(), v.getRepartidorUsuarioId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("No hay pedidos listos para salir."));
        return tomarPedidoComo(primero.getId(), cadete);
    }

    @Transactional(readOnly = true)
    public Venta obtenerPedidoPorTelefono(String telefonoRaw) {
        String tenant = TenantContext.getCurrentTenant();
        String telefono = TelefonoWhatsApp.normalizar(telefonoRaw);
        if (telefono == null) {
            return null;
        }
        List<Venta> ventas = ventaRepository.findByTenantIdAndTelefonoContactoOrderByFechaDesc(tenant, telefono);
        if (ventas.isEmpty() && !telefono.equals(telefonoRaw)) {
            ventas = ventaRepository.findByTenantIdAndTelefonoContactoOrderByFechaDesc(tenant, telefonoRaw);
        }
        return ventas.stream()
                .filter(v -> !EstadoPedido.ANULADA.equals(v.getEstado()))
                .findFirst()
                .orElse(null);
    }

    private Venta resolverVentaParaTomar(Long id, String tenant) {
        return ventaRepository.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con id: " + id));
    }

    private void exigirCadeteDuenio(Usuario cadete, Long ventaId) {
        if (cadete == null || cadete.getRol() != Rol.REPARTIDOR) {
            throw new BusinessException("Solo un cadete puede hacer eso.");
        }
        String tenant = TenantContext.getCurrentTenant();
        Venta venta = ventaRepository.findByIdAndTenantId(ventaId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con id: " + ventaId));
        if (venta.getRepartidorUsuarioId() == null || !venta.getRepartidorUsuarioId().equals(cadete.getId())) {
            throw new BusinessException("Este envío no es tuyo.");
        }
    }

    private List<VentaListadoDTO> mapearListado(List<Venta> ventas, Map<Long, Venta> hidratados) {
        return ventas.stream()
                .map(v -> toListado(hidratados.getOrDefault(v.getId(), v)))
                .toList();
    }

    private Map<Long, Venta> hidratarItems(List<Venta> ventas) {
        if (ventas == null || ventas.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = ventas.stream().map(Venta::getId).toList();
        return ventaRepository.findWithItemsByIdIn(ids).stream()
                .collect(Collectors.toMap(Venta::getId, v -> v, (a, b) -> a));
    }

    private List<Venta> concatenar(List<Venta> a, List<Venta> b) {
        List<Venta> todas = new ArrayList<>();
        if (a != null) {
            todas.addAll(a);
        }
        if (b != null) {
            todas.addAll(b);
        }
        return todas;
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

    private void asegurarMesaLibre(Mesa mesa, Long ventaExcluirId) {
        List<Venta> abiertas = ventaRepository.findCuentasAbiertasByMesa(
                TenantContext.getCurrentTenant(), mesa.getId());
        boolean hayOtra = abiertas.stream()
                .anyMatch(v -> ventaExcluirId == null || !ventaExcluirId.equals(v.getId()));
        if (hayOtra) {
            throw new BusinessException(mesa.etiqueta() + " ya tiene una cuenta abierta.");
        }
    }

    private void marcarMesaOcupada(Mesa mesa) {
        mesa.setEstado(Mesa.OCUPADA);
        mesaRepository.save(mesa);
    }

    private void liberarMesaSiCorresponde(Venta venta) {
        if (venta.getMesa() == null) {
            return;
        }
        Mesa mesa = venta.getMesa();
        List<Venta> otras = ventaRepository.findCuentasAbiertasByMesa(
                TenantContext.getCurrentTenant(), mesa.getId());
        boolean quedaAbierta = otras.stream().anyMatch(v -> !v.getId().equals(venta.getId()));
        if (!quedaAbierta) {
            mesa.setEstado(Mesa.LIBRE);
            mesaRepository.save(mesa);
        }
    }
}

package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.controllers.CajaDescargoDTO;
import com.tulumcore.api.entities.Caja;
import com.tulumcore.api.entities.CajaDescargo;
import com.tulumcore.api.entities.PagoRemito;
import com.tulumcore.api.entities.Usuario;
import com.tulumcore.api.entities.Venta;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.repositories.CajaDescargoRepository;
import com.tulumcore.api.repositories.CajaRepository;
import com.tulumcore.api.repositories.PagoRemitoRepository;
import com.tulumcore.api.repositories.VentaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CajaService {

    private static final Logger log = LoggerFactory.getLogger(CajaService.class);

    public static final String EFECTIVO = "EFECTIVO";
    public static final String TRANSFERENCIA = "TRANSFERENCIA";
    public static final String MERCADO_PAGO = "MERCADO_PAGO";

    @Autowired
    private CajaRepository cajaRepository;

    @Autowired
    private CajaDescargoRepository cajaDescargoRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private PagoRemitoRepository pagoRemitoRepository;

    @Autowired
    private AuditoryLogService auditoryLogService;

    @Value("${app.caja.max-horas-abierta:24}")
    private int maxHorasAbierta;

    private Optional<Caja> findAbierta() {
        String tenant = TenantContext.getCurrentTenant();
        return cajaRepository.findByEstadoAndTenantId("ABIERTA", tenant);
    }

    public boolean estaVencida(Caja caja) {
        if (caja == null || caja.getFechaApertura() == null || !"ABIERTA".equals(caja.getEstado())) {
            return false;
        }
        long minutos = Duration.between(caja.getFechaApertura(), LocalDateTime.now()).toMinutes();
        return minutos >= (long) maxHorasAbierta * 60;
    }

    /**
     * Si el turno ya cumplió el día, lo cierra con el esperado del sistema.
     * Devuelve empty cuando no hay caja operativa (cerrada o recién auto-cerrada).
     */
    private Optional<Caja> resolverCajaOperativa() {
        Optional<Caja> abierta = findAbierta();
        if (abierta.isEmpty()) {
            return Optional.empty();
        }
        Caja caja = abierta.get();
        if (estaVencida(caja)) {
            persistirCierreAutomatico(caja);
            return Optional.empty();
        }
        return Optional.of(marcarLimiteTurno(caja));
    }

    @Transactional
    public Optional<Caja> obtenerCajaAbierta() {
        return resolverCajaOperativa();
    }

    /**
     * Devuelve la caja abierta con los buckets reconstruidos desde las ventas
     * y cobranzas del turno. El arqueo no debe usar el historial completo.
     */
    @Transactional
    public Optional<Caja> obtenerCajaAbiertaActualizada() {
        return resolverCajaOperativa().map(caja -> marcarLimiteTurno(reconstruirTurno(caja)));
    }

    /**
     * Caja abierta y dentro de las 24h. Si el turno se venció, se cierra solo
     * y hay que abrir uno nuevo.
     */
    @Transactional
    public Caja exigirCajaOperativa() {
        return resolverCajaOperativa()
                .orElseThrow(() -> new BusinessException(
                        "Debe abrir caja para realizar esta operacion. "
                                + "Si el turno anterior cumplió "
                                + maxHorasAbierta
                                + " horas, se cerró automáticamente."));
    }

    public Caja marcarLimiteTurno(Caja caja) {
        if (caja == null) {
            return null;
        }
        caja.setLimiteHoras(maxHorasAbierta);
        if (caja.getFechaApertura() == null || !"ABIERTA".equals(caja.getEstado())) {
            caja.setHorasAbierta(0.0);
            caja.setExpirada(false);
            return caja;
        }
        double horas = Duration.between(caja.getFechaApertura(), LocalDateTime.now()).toMinutes() / 60.0;
        caja.setHorasAbierta(Math.round(horas * 10.0) / 10.0);
        caja.setExpirada(estaVencida(caja));
        return caja;
    }

    public List<Caja> obtenerHistorial() {
        String tenant = TenantContext.getCurrentTenant();
        List<Caja> cajas = cajaRepository.findAllByTenantIdOrderByFechaAperturaDesc(tenant);
        for (Caja caja : cajas) {
            caja.setDescargos(cajaDescargoRepository.findAllByTenantIdAndCajaIdOrderByFechaDesc(tenant, caja.getId()));
            marcarLimiteTurno(caja);
        }
        return cajas;
    }

    @Transactional
    public Caja abrirCaja(Double montoInicial) {
        String tenant = TenantContext.getCurrentTenant();
        Usuario usuario = auditoryLogService.getCurrentUser();

        Optional<Caja> abierta = findAbierta();
        if (abierta.isPresent()) {
            Caja caja = abierta.get();
            if (estaVencida(caja)) {
                persistirCierreAutomatico(caja);
            } else {
                throw new BusinessException("Ya existe una caja abierta para este comercio.");
            }
        }

        Caja nuevaCaja = new Caja();
        nuevaCaja.setFechaApertura(LocalDateTime.now());
        nuevaCaja.setMontoInicial(montoInicial);
        nuevaCaja.setMontoVentasEfectivo(0.0);
        nuevaCaja.setMontoVentasMP(0.0);
        nuevaCaja.setMontoVentasTransferencia(0.0);
        nuevaCaja.setMontoCobranzasEfectivo(0.0);
        nuevaCaja.setMontoCobranzasTransferencia(0.0);
        nuevaCaja.setMontoFinalEsperado(montoInicial);
        nuevaCaja.setEstado("ABIERTA");
        nuevaCaja.setCierreAutomatico(false);
        nuevaCaja.setTenantId(tenant);
        nuevaCaja.setUsuarioApertura(usuario);

        Caja saved = cajaRepository.save(nuevaCaja);
        auditoryLogService.registrar("CREATE", "CAJA", saved.getId(),
                "Caja abierta", null, detalleCaja(saved));
        return marcarLimiteTurno(saved);
    }

    @Transactional
    public Caja cerrarCaja(Double montoFinalReal) {
        Caja caja = findAbierta()
                .orElseThrow(() -> new ResourceNotFoundException("No hay una caja abierta para cerrar."));

        reconstruirTurno(caja);
        String detalleAnterior = detalleCaja(caja);
        caja.setFechaCierre(LocalDateTime.now());
        caja.setMontoFinalReal(montoFinalReal);
        caja.setEstado("CERRADA");
        caja.setCierreAutomatico(false);
        caja.setMotivoCierre("Cierre manual con arqueo");

        Caja saved = cajaRepository.save(caja);
        auditoryLogService.registrar("UPDATE", "CAJA", saved.getId(),
                "Caja cerrada", detalleAnterior, detalleCaja(saved));
        return saved;
    }

    /**
     * Cierra un turno vencido de cualquier tenant. El job setea TenantContext antes.
     */
    @Transactional
    public void cerrarTurnoAutomaticoPorId(Long id) {
        Caja caja = cajaRepository.findById(id).orElse(null);
        if (caja == null || !"ABIERTA".equals(caja.getEstado()) || !estaVencida(caja)) {
            return;
        }
        persistirCierreAutomatico(caja);
    }

    private void persistirCierreAutomatico(Caja caja) {
        reconstruirTurno(caja);
        String detalleAnterior = detalleCaja(caja);
        double esperado = nz(caja.getMontoFinalEsperado());
        caja.setFechaCierre(LocalDateTime.now());
        caja.setMontoFinalReal(esperado);
        caja.setEstado("CERRADA");
        caja.setCierreAutomatico(true);
        caja.setMotivoCierre("Cierre automatico al cumplir "
                + maxHorasAbierta
                + " horas. El efectivo se tomo como el esperado del sistema. "
                + "Si hay diferencia, registrar un descargo.");
        Caja saved = cajaRepository.save(caja);
        log.info("Caja {} del tenant {} cerrada automaticamente tras {}h. Esperado/real={}",
                saved.getId(), saved.getTenantId(), maxHorasAbierta, esperado);
        auditoryLogService.registrar("UPDATE", "CAJA", saved.getId(),
                "Caja cerrada automaticamente", detalleAnterior, detalleCaja(saved));
    }

    @Transactional
    public Caja registrarDescargo(Long cajaId, CajaDescargoDTO dto) {
        if (dto == null || dto.getMontoFinalReal() == null) {
            throw new BusinessException("Indicá el monto real de efectivo para el descargo.");
        }
        if (dto.getMontoFinalReal() < 0) {
            throw new BusinessException("El monto del descargo no puede ser negativo.");
        }
        String motivo = dto.getMotivo() != null ? dto.getMotivo().trim() : "";
        if (motivo.length() < 8) {
            throw new BusinessException("El descargo necesita un motivo (mínimo 8 caracteres).");
        }

        String tenant = TenantContext.getCurrentTenant();
        Caja caja = cajaRepository.findById(cajaId)
                .orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada."));
        if (!tenant.equals(caja.getTenantId())) {
            throw new ResourceNotFoundException("Caja no encontrada.");
        }
        if (!"CERRADA".equals(caja.getEstado())) {
            throw new BusinessException("Solo se puede descargar una caja ya cerrada.");
        }

        double anterior = nz(caja.getMontoFinalReal());
        double nuevo = dto.getMontoFinalReal();
        String detalleAnterior = detalleCaja(caja);

        caja.setMontoFinalReal(nuevo);
        Caja saved = cajaRepository.save(caja);

        CajaDescargo descargo = new CajaDescargo();
        descargo.setTenantId(tenant);
        descargo.setCaja(saved);
        descargo.setFecha(LocalDateTime.now());
        descargo.setMontoAnterior(anterior);
        descargo.setMontoNuevo(nuevo);
        descargo.setDiferencia(Math.round((nuevo - anterior) * 100.0) / 100.0);
        descargo.setMotivo(motivo);
        try {
            descargo.setUsuario(auditoryLogService.getCurrentUser());
        } catch (RuntimeException ignored) {
            // El descargo queda registrado aunque no haya usuario persistido.
        }
        cajaDescargoRepository.save(descargo);

        auditoryLogService.registrar("UPDATE", "CAJA_DESCARGO", saved.getId(),
                "Descargo de caja: " + motivo, detalleAnterior, detalleCaja(saved));

        saved.setDescargos(cajaDescargoRepository.findAllByTenantIdAndCajaIdOrderByFechaDesc(tenant, saved.getId()));
        return saved;
    }

    /**
     * Recalcula el efectivo esperado en el cajon: monto inicial + ventas en efectivo
     * + cobranzas de remitos en efectivo. Las transferencias y Mercado Pago quedan afuera.
     */
    public double recalcularMontoFinalEsperado(Caja caja) {
        double esperado = nz(caja.getMontoInicial())
                + nz(caja.getMontoVentasEfectivo())
                + nz(caja.getMontoCobranzasEfectivo());
        caja.setMontoFinalEsperado(esperado);
        return esperado;
    }

    /**
     * Pisa los acumuladores del turno con lo que realmente se vendio y cobro
     * desde la apertura. Asi una venta que no entro a ningun bucket se corrige sola.
     */
    @Transactional
    public Caja reconstruirTurno(Caja caja) {
        if (caja == null || caja.getFechaApertura() == null) {
            return caja;
        }

        String tenant = caja.getTenantId() != null ? caja.getTenantId() : TenantContext.getCurrentTenant();
        LocalDateTime desde = caja.getFechaApertura();

        double ventasEfectivo = 0;
        double ventasTransferencia = 0;
        double ventasMp = 0;
        for (Venta venta : ventaRepository.findByTenantIdAndFechaGreaterThanEqual(tenant, desde)) {
            if ("ANULADA".equals(venta.getEstado())) {
                continue;
            }
            double total = nz(venta.getTotalFinal());
            String metodo = normalizarMetodo(venta.getMetodoPago());
            if (EFECTIVO.equals(metodo)) {
                ventasEfectivo += total;
            } else if (TRANSFERENCIA.equals(metodo)) {
                ventasTransferencia += total;
            } else {
                ventasMp += total;
            }
        }

        double cobranzasEfectivo = 0;
        double cobranzasTransferencia = 0;
        for (PagoRemito pago : pagoRemitoRepository.findAllByTenantIdAndFechaGreaterThanEqual(tenant, desde)) {
            double monto = nz(pago.getMonto());
            if (EFECTIVO.equals(normalizarMetodo(pago.getMetodoPago()))) {
                cobranzasEfectivo += monto;
            } else {
                cobranzasTransferencia += monto;
            }
        }

        caja.setMontoVentasEfectivo(ventasEfectivo);
        caja.setMontoVentasTransferencia(ventasTransferencia);
        caja.setMontoVentasMP(ventasMp);
        caja.setMontoCobranzasEfectivo(cobranzasEfectivo);
        caja.setMontoCobranzasTransferencia(cobranzasTransferencia);
        recalcularMontoFinalEsperado(caja);
        return cajaRepository.save(caja);
    }

    private String normalizarMetodo(String metodoPago) {
        if (metodoPago == null || metodoPago.isBlank()) {
            return MERCADO_PAGO;
        }
        String normalizado = metodoPago.trim().toUpperCase();
        if (EFECTIVO.equals(normalizado) || TRANSFERENCIA.equals(normalizado)) {
            return normalizado;
        }
        return MERCADO_PAGO;
    }

    private double nz(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private String detalleCaja(Caja caja) {
        return auditoryLogService.detalle(
                "estado", caja.getEstado(),
                "cierreAutomatico", caja.getCierreAutomatico(),
                "motivoCierre", caja.getMotivoCierre(),
                "montoInicial", caja.getMontoInicial(),
                "montoVentasEfectivo", caja.getMontoVentasEfectivo(),
                "montoVentasMP", caja.getMontoVentasMP(),
                "montoVentasTransferencia", caja.getMontoVentasTransferencia(),
                "montoCobranzasEfectivo", caja.getMontoCobranzasEfectivo(),
                "montoCobranzasTransferencia", caja.getMontoCobranzasTransferencia(),
                "montoFinalEsperado", caja.getMontoFinalEsperado(),
                "montoFinalReal", caja.getMontoFinalReal()
        );
    }
}

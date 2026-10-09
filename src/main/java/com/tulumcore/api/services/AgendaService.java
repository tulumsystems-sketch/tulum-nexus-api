package com.tulumcore.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.controllers.AgendaConfigDTO;
import com.tulumcore.api.controllers.AgendaHuecoDTO;
import com.tulumcore.api.controllers.AgendaHuecosDiaDTO;
import com.tulumcore.api.controllers.BotContextoDTO;
import com.tulumcore.api.controllers.ItemVentaDTO;
import com.tulumcore.api.controllers.TurnoDTO;
import com.tulumcore.api.controllers.TurnoListadoDTO;
import com.tulumcore.api.controllers.VentaDTO;
import com.tulumcore.api.entities.Cliente;
import com.tulumcore.api.entities.FeatureKey;
import com.tulumcore.api.entities.Producto;
import com.tulumcore.api.entities.TenantConfig;
import com.tulumcore.api.entities.Turno;
import com.tulumcore.api.entities.Venta;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.repositories.ClienteRepository;
import com.tulumcore.api.repositories.ProductoRepository;
import com.tulumcore.api.repositories.TenantConfigRepository;
import com.tulumcore.api.repositories.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AgendaService {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<String> ESTADOS = Set.of(
            Turno.PEDIDO, Turno.CONFIRMADO, Turno.EN_ESPERA, Turno.ATENDIDO, Turno.AUSENTE, Turno.CANCELADO
    );

    private final TenantFeatureService tenantFeatureService;
    private final TurnoRepository turnoRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final TenantConfigRepository tenantConfigRepository;
    private final VentaService ventaService;
    private final ObjectMapper objectMapper;

    public AgendaService(
            TenantFeatureService tenantFeatureService,
            TurnoRepository turnoRepository,
            ClienteRepository clienteRepository,
            ProductoRepository productoRepository,
            TenantConfigRepository tenantConfigRepository,
            VentaService ventaService,
            ObjectMapper objectMapper
    ) {
        this.tenantFeatureService = tenantFeatureService;
        this.turnoRepository = turnoRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.tenantConfigRepository = tenantConfigRepository;
        this.ventaService = ventaService;
        this.objectMapper = objectMapper;
    }

    public BotContextoDTO contexto() {
        String tenant = TenantContext.getCurrentTenant();
        TenantConfig cfg = tenant == null ? null : tenantConfigRepository.findByTenantId(tenant).orElse(null);
        return new BotContextoDTO(
                tenant,
                cfg != null ? cfg.getNombreEmpresa() : tenant,
                tenantFeatureService.isEnabled(FeatureKey.AGENDA),
                tenantFeatureService.isEnabled(FeatureKey.MESAS),
                tenantFeatureService.isEnabled(FeatureKey.WHATSAPP_BOT)
        );
    }

    public AgendaConfigDTO leerConfig() {
        exigirAgenda();
        String raw = tenantFeatureService.configurationJson(FeatureKey.AGENDA);
        if (!StringUtils.hasText(raw)) {
            return AgendaConfigDTO.porDefecto();
        }
        try {
            AgendaConfigDTO dto = objectMapper.readValue(raw, AgendaConfigDTO.class);
            return normalizarConfig(dto);
        } catch (Exception ex) {
            return AgendaConfigDTO.porDefecto();
        }
    }

    @Transactional
    public AgendaConfigDTO guardarConfig(AgendaConfigDTO dto) {
        exigirAgenda();
        AgendaConfigDTO limpio = normalizarConfig(dto);
        try {
            tenantFeatureService.saveConfigurationJson(FeatureKey.AGENDA, objectMapper.writeValueAsString(limpio));
        } catch (Exception ex) {
            throw new BusinessException("No pudimos guardar el horario.");
        }
        return limpio;
    }

    public List<TurnoListadoDTO> listar(LocalDate desde, LocalDate hasta) {
        exigirAgenda();
        AgendaConfigDTO cfg = leerConfig();
        ZoneId zona = zona(cfg);
        LocalDate d0 = desde != null ? desde : LocalDate.now(zona);
        LocalDate exclusiveEnd;
        if (hasta == null || !hasta.isAfter(d0)) {
            exclusiveEnd = d0.plusDays(1);
        } else {
            exclusiveEnd = hasta.plusDays(1);
        }
        Instant inicio = d0.atStartOfDay(zona).toInstant();
        Instant fin = exclusiveEnd.atStartOfDay(zona).toInstant();
        return turnoRepository
                .findByTenantIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(tenant(), inicio, fin)
                .stream()
                .map(this::toListado)
                .toList();
    }

    public TurnoListadoDTO obtener(Long id) {
        exigirAgenda();
        return toListado(exigir(id));
    }

    public List<TurnoListadoDTO> mios(String telefono) {
        exigirAgenda();
        Cliente paciente = buscarPorTelefono(telefono);
        if (paciente == null) {
            return List.of();
        }
        Instant corte = Instant.now().minusSeconds(12 * 3600);
        return turnoRepository.findByTenantIdAndCliente_IdOrderByInicioDesc(tenant(), paciente.getId()).stream()
                .filter(t -> t.getInicio() != null && t.getInicio().isAfter(corte))
                .filter(t -> !Turno.CANCELADO.equals(t.getEstado()) && !Turno.AUSENTE.equals(t.getEstado()) && !Turno.ATENDIDO.equals(t.getEstado()))
                .map(this::toListado)
                .toList();
    }

    public List<AgendaHuecosDiaDTO> huecosProximos(int dias) {
        exigirAgenda();
        AgendaConfigDTO cfg = leerConfig();
        ZoneId zona = zona(cfg);
        LocalDate hoy = LocalDate.now(zona);
        int n = Math.max(1, Math.min(dias, 14));
        List<AgendaHuecosDiaDTO> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            LocalDate fecha = hoy.plusDays(i);
            out.add(new AgendaHuecosDiaDTO(fecha.toString(), huecosDeFecha(fecha, cfg)));
        }
        return out;
    }

    public AgendaHuecosDiaDTO huecos(LocalDate fecha) {
        exigirAgenda();
        AgendaConfigDTO cfg = leerConfig();
        LocalDate dia = fecha != null ? fecha : LocalDate.now(zona(cfg));
        return new AgendaHuecosDiaDTO(dia.toString(), huecosDeFecha(dia, cfg));
    }

    @Transactional
    public TurnoListadoDTO crear(TurnoDTO dto, boolean confirmar) {
        exigirAgenda();
        if (dto == null || dto.getInicio() == null) {
            throw new BusinessException("Indicá el horario del turno.");
        }
        AgendaConfigDTO cfg = leerConfig();
        int minutos = dto.getDuracionMinutos() != null && dto.getDuracionMinutos() > 0
                ? dto.getDuracionMinutos()
                : cfg.duracionMinutos();
        Instant inicio = dto.getInicio();
        Instant fin = inicio.plusSeconds(minutos * 60L);
        exigirHueco(inicio, fin, null);

        Cliente paciente = resolverPaciente(dto);
        Turno turno = new Turno();
        turno.setTenantId(tenant());
        turno.setCliente(paciente);
        turno.setProducto(resolverPractica(dto.getProductoId()));
        turno.setInicio(inicio);
        turno.setFin(fin);
        turno.setDuracionMinutos(minutos);
        turno.setEstado(confirmar ? Turno.CONFIRMADO : estadoInicial(dto.getEstado(), confirmar));
        turno.setModalidad(normalizarModalidad(dto.getModalidad()));
        turno.setNotas(texto(dto.getNotas()));
        return toListado(turnoRepository.save(turno));
    }

    @Transactional
    public TurnoListadoDTO actualizar(Long id, TurnoDTO dto) {
        exigirAgenda();
        Turno turno = exigir(id);
        if (dto == null) {
            return toListado(turno);
        }
        if (dto.getInicio() != null) {
            AgendaConfigDTO cfg = leerConfig();
            int minutos = dto.getDuracionMinutos() != null && dto.getDuracionMinutos() > 0
                    ? dto.getDuracionMinutos()
                    : turno.getDuracionMinutos();
            Instant inicio = dto.getInicio();
            Instant fin = inicio.plusSeconds(minutos * 60L);
            exigirHueco(inicio, fin, turno.getId());
            turno.setInicio(inicio);
            turno.setFin(fin);
            turno.setDuracionMinutos(minutos);
        }
        if (dto.getEstado() != null) {
            turno.setEstado(normalizarEstado(dto.getEstado()));
        }
        if (dto.getModalidad() != null) {
            turno.setModalidad(normalizarModalidad(dto.getModalidad()));
        }
        if (dto.getNotas() != null) {
            turno.setNotas(texto(dto.getNotas()));
        }
        if (dto.getProductoId() != null) {
            turno.setProducto(resolverPractica(dto.getProductoId()));
        }
        if (dto.getClienteId() != null || texto(dto.getTelefono()) != null || texto(dto.getNombre()) != null) {
            turno.setCliente(resolverPaciente(dto));
        }
        return toListado(turnoRepository.save(turno));
    }

    @Transactional
    public TurnoListadoDTO cambiarEstado(Long id, String estado) {
        exigirAgenda();
        Turno turno = exigir(id);
        turno.setEstado(normalizarEstado(estado));
        return toListado(turnoRepository.save(turno));
    }

    @Transactional
    public TurnoListadoDTO confirmarMio(Long id, String telefono) {
        Turno turno = exigirMio(id, telefono);
        if (Turno.CANCELADO.equals(turno.getEstado())) {
            throw new BusinessException("Ese turno ya está cancelado.");
        }
        turno.setEstado(Turno.CONFIRMADO);
        return toListado(turnoRepository.save(turno));
    }

    @Transactional
    public TurnoListadoDTO cancelarMio(Long id, String telefono) {
        Turno turno = exigirMio(id, telefono);
        turno.setEstado(Turno.CANCELADO);
        return toListado(turnoRepository.save(turno));
    }

    @Transactional
    public TurnoListadoDTO cobrar(Long id, String metodoPago) {
        exigirAgenda();
        Turno turno = exigir(id);
        if (turno.getVentaId() != null) {
            throw new BusinessException("Este turno ya está cobrado.");
        }
        Producto practica = turno.getProducto();
        if (practica == null) {
            practica = primeraPractica();
        }
        if (practica == null) {
            throw new BusinessException("Cargá una práctica (Consulta) en Prácticas para poder cobrar.");
        }

        VentaDTO ventaDto = new VentaDTO();
        ventaDto.setCanal(CanalVenta.MOSTRADOR);
        ventaDto.setMetodoPago(metodoPago);
        ventaDto.setClienteId(turno.getCliente().getId());
        ventaDto.setNombreContacto(nombrePaciente(turno.getCliente()));
        ventaDto.setTelefonoContacto(turno.getCliente().getTelefono());
        ventaDto.setObservaciones("Consulta · turno #" + turno.getId());
        ventaDto.setCobrado(true);
        ItemVentaDTO item = new ItemVentaDTO();
        item.setProductoId(practica.getId());
        item.setCantidad(1);
        ventaDto.setItems(List.of(item));

        Venta venta = ventaService.guardar(ventaDto);
        turno.setVentaId(venta.getId());
        if (!Turno.ATENDIDO.equals(turno.getEstado()) && !Turno.EN_ESPERA.equals(turno.getEstado())) {
            turno.setEstado(Turno.ATENDIDO);
        }
        turno.setProducto(practica);
        return toListado(turnoRepository.save(turno));
    }

    public List<TurnoListadoDTO> recordatoriosPendientes() {
        if (!tenantFeatureService.isEnabled(FeatureKey.AGENDA)) {
            return List.of();
        }
        Instant ahora = Instant.now();
        List<TurnoListadoDTO> out = new ArrayList<>();
        for (Turno t : turnoRepository.recordatoriosVentana(tenant(), ahora, ahora.plusSeconds(26 * 3600))) {
            long secs = t.getInicio().getEpochSecond() - ahora.getEpochSecond();
            if (!t.isRecordatorio24h() && secs > 12 * 3600 && secs <= 26 * 3600) {
                out.add(toListado(t));
            } else if (!t.isRecordatorio2h() && secs > 30 * 60 && secs <= 3 * 3600) {
                out.add(toListado(t));
            }
        }
        return out;
    }

    @Transactional
    public TurnoListadoDTO marcarRecordatorio(Long id, String tipo) {
        exigirAgenda();
        Turno turno = exigir(id);
        if ("2h".equalsIgnoreCase(tipo) || "hora".equalsIgnoreCase(tipo)) {
            turno.setRecordatorio2h(true);
        } else {
            turno.setRecordatorio24h(true);
        }
        return toListado(turnoRepository.save(turno));
    }

    private Turno exigirMio(Long id, String telefono) {
        exigirAgenda();
        Turno turno = exigir(id);
        Cliente paciente = buscarPorTelefono(telefono);
        if (paciente == null || turno.getCliente() == null || !paciente.getId().equals(turno.getCliente().getId())) {
            throw new BusinessException("Ese turno no es tuyo.");
        }
        return turno;
    }

    private List<AgendaHuecoDTO> huecosDeFecha(LocalDate fecha, AgendaConfigDTO cfg) {
        ZoneId zona = zona(cfg);
        DayOfWeek dow = fecha.getDayOfWeek();
        int iso = dow.getValue();
        if (cfg.diasSemana() == null || !cfg.diasSemana().contains(iso)) {
            return List.of();
        }
        LocalTime abre = parseHora(cfg.horaInicio(), LocalTime.of(9, 0));
        LocalTime cierra = parseHora(cfg.horaFin(), LocalTime.of(18, 0));
        if (!cierra.isAfter(abre)) {
            return List.of();
        }
        int minutos = Math.max(5, cfg.duracionMinutos());
        Instant diaInicio = fecha.atStartOfDay(zona).toInstant();
        Instant diaFin = fecha.plusDays(1).atStartOfDay(zona).toInstant();
        List<Turno> ocupados = turnoRepository
                .findByTenantIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(tenant(), diaInicio, diaFin)
                .stream()
                .filter(Turno::ocupaAgenda)
                .toList();

        Instant ahora = Instant.now();
        List<AgendaHuecoDTO> huecos = new ArrayList<>();
        ZonedDateTime cursor = fecha.atTime(abre).atZone(zona);
        ZonedDateTime limite = fecha.atTime(cierra).atZone(zona);
        while (!cursor.plusMinutes(minutos).isAfter(limite)) {
            Instant ini = cursor.toInstant();
            Instant fin = cursor.plusMinutes(minutos).toInstant();
            if (!ini.isBefore(ahora) && libre(ocupados, ini, fin)) {
                huecos.add(new AgendaHuecoDTO(ini, fin, minutos));
            }
            cursor = cursor.plusMinutes(minutos);
        }
        return huecos;
    }

    private boolean libre(List<Turno> ocupados, Instant inicio, Instant fin) {
        for (Turno t : ocupados) {
            if (t.getInicio().isBefore(fin) && t.getFin().isAfter(inicio)) {
                return false;
            }
        }
        return true;
    }

    private void exigirHueco(Instant inicio, Instant fin, Long excludeId) {
        List<Turno> choque = turnoRepository.solapados(tenant(), inicio, fin, excludeId);
        if (!choque.isEmpty()) {
            throw new BusinessException("Ese horario ya está tomado.");
        }
    }

    private Cliente resolverPaciente(TurnoDTO dto) {
        String tenant = tenant();
        if (dto.getClienteId() != null && dto.getClienteId() > 0) {
            return clienteRepository.findByIdAndTenantId(dto.getClienteId(), tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado."));
        }
        String tel = TelefonoWhatsApp.normalizar(dto.getTelefono());
        Cliente porTel = tel != null ? buscarPorTelefono(tel) : null;
        if (porTel != null) {
            aplicarFicha(porTel, dto);
            return clienteRepository.save(porTel);
        }
        String dni = texto(dto.getDni());
        if (dni != null) {
            Cliente porDni = clienteRepository.findFirstByTenantIdAndDniIgnoreCase(tenant, dni).orElse(null);
            if (porDni != null) {
                aplicarFicha(porDni, dto);
                return clienteRepository.save(porDni);
            }
        }
        if (!StringUtils.hasText(dto.getNombre())) {
            throw new BusinessException("Falta el nombre del paciente.");
        }
        Cliente nuevo = new Cliente();
        nuevo.setTenantId(tenant);
        aplicarFicha(nuevo, dto);
        if (!StringUtils.hasText(nuevo.getApellido())) {
            nuevo.setApellido("-");
        }
        return clienteRepository.save(nuevo);
    }

    private void aplicarFicha(Cliente c, TurnoDTO dto) {
        if (texto(dto.getNombre()) != null) {
            c.setNombre(texto(dto.getNombre()));
        }
        if (texto(dto.getApellido()) != null) {
            c.setApellido(texto(dto.getApellido()));
        }
        String tel = TelefonoWhatsApp.normalizar(dto.getTelefono());
        if (tel != null) {
            c.setTelefono(tel);
        }
        if (texto(dto.getDni()) != null) {
            c.setDni(texto(dto.getDni()));
        }
        if (texto(dto.getObraSocial()) != null) {
            c.setObraSocial(texto(dto.getObraSocial()));
        }
        if (texto(dto.getNroAfiliado()) != null) {
            c.setNroAfiliado(texto(dto.getNroAfiliado()));
        }
    }

    private Cliente buscarPorTelefono(String telefono) {
        String n = TelefonoWhatsApp.normalizar(telefono);
        if (n == null) {
            return null;
        }
        List<Cliente> exactos = clienteRepository.findByTenantIdAndTelefono(tenant(), n);
        if (!exactos.isEmpty()) {
            return exactos.get(0);
        }
        for (Cliente c : clienteRepository.findAllByTenantId(tenant())) {
            if (TelefonoWhatsApp.mismaLinea(n, c.getTelefono())) {
                return c;
            }
        }
        return null;
    }

    private Producto resolverPractica(Long productoId) {
        if (productoId == null) {
            return null;
        }
        return productoRepository.findByIdAndTenantId(productoId, tenant())
                .orElseThrow(() -> new ResourceNotFoundException("Práctica no encontrada."));
    }

    private Producto primeraPractica() {
        return productoRepository.findAllByTenantId(tenant()).stream()
                .filter(Producto::isVendible)
                .findFirst()
                .orElse(null);
    }

    private Turno exigir(Long id) {
        return turnoRepository.findByIdAndTenantId(id, tenant())
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado."));
    }

    private TurnoListadoDTO toListado(Turno t) {
        Cliente c = t.getCliente();
        Producto p = t.getProducto();
        return new TurnoListadoDTO(
                t.getId(),
                t.getInicio(),
                t.getFin(),
                t.getDuracionMinutos(),
                t.getEstado(),
                t.getModalidad(),
                t.getNotas(),
                c != null ? c.getId() : null,
                c != null ? nombrePaciente(c) : null,
                c != null ? c.getDni() : null,
                c != null ? c.getTelefono() : null,
                c != null ? c.getObraSocial() : null,
                p != null ? p.getId() : null,
                p != null ? p.getNombre() : null,
                p != null ? p.getPrecio() : null,
                t.getVentaId(),
                t.getVentaId() != null
        );
    }

    private String nombrePaciente(Cliente c) {
        String n = (c.getNombre() == null ? "" : c.getNombre().trim());
        String a = (c.getApellido() == null ? "" : c.getApellido().trim());
        return (n + " " + a).trim();
    }

    private AgendaConfigDTO normalizarConfig(AgendaConfigDTO dto) {
        AgendaConfigDTO base = AgendaConfigDTO.porDefecto();
        if (dto == null) {
            return base;
        }
        int dur = dto.duracionMinutos() > 0 ? dto.duracionMinutos() : base.duracionMinutos();
        String hi = texto(dto.horaInicio()) != null ? dto.horaInicio() : base.horaInicio();
        String hf = texto(dto.horaFin()) != null ? dto.horaFin() : base.horaFin();
        List<Integer> dias = dto.diasSemana() == null || dto.diasSemana().isEmpty()
                ? base.diasSemana()
                : dto.diasSemana().stream().filter(d -> d != null && d >= 1 && d <= 7).distinct().toList();
        if (dias.isEmpty()) {
            dias = base.diasSemana();
        }
        String zona = texto(dto.zona()) != null ? dto.zona() : base.zona();
        try {
            ZoneId.of(zona);
        } catch (Exception ex) {
            zona = base.zona();
        }
        return new AgendaConfigDTO(dur, hi, hf, dias, zona);
    }

    private ZoneId zona(AgendaConfigDTO cfg) {
        try {
            return ZoneId.of(cfg.zona());
        } catch (Exception ex) {
            return ZoneId.of("America/Argentina/Buenos_Aires");
        }
    }

    private LocalTime parseHora(String raw, LocalTime fallback) {
        try {
            return LocalTime.parse(raw, HORA);
        } catch (DateTimeParseException ex) {
            return fallback;
        }
    }

    private String estadoInicial(String pedido, boolean confirmar) {
        if (confirmar) {
            return Turno.CONFIRMADO;
        }
        if (texto(pedido) != null) {
            return normalizarEstado(pedido);
        }
        return Turno.PEDIDO;
    }

    private String normalizarEstado(String raw) {
        String e = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if ("ESPERA".equals(e) || "SALA".equals(e)) {
            e = Turno.EN_ESPERA;
        }
        if ("NO_VINO".equals(e) || "FALTO".equals(e) || "FALTÓ".equals(e)) {
            e = Turno.AUSENTE;
        }
        if (!ESTADOS.contains(e)) {
            throw new BusinessException("Estado de turno inválido.");
        }
        return e;
    }

    private String normalizarModalidad(String raw) {
        String m = raw == null ? Turno.PARTICULAR : raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (m.contains("OBRA")) {
            return Turno.OBRA_SOCIAL;
        }
        return Turno.PARTICULAR;
    }

    private void exigirAgenda() {
        tenantFeatureService.requireEnabled(FeatureKey.AGENDA);
    }

    private String tenant() {
        String t = TenantContext.getCurrentTenant();
        if (t == null || t.isBlank()) {
            throw new BusinessException("Falta el comercio.");
        }
        return t;
    }

    private String texto(String valor) {
        if (valor == null) {
            return null;
        }
        String recorte = valor.trim();
        return recorte.isEmpty() ? null : recorte;
    }
}

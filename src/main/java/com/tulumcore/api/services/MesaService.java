package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.controllers.MesaCuentaDTO;
import com.tulumcore.api.controllers.MesaDTO;
import com.tulumcore.api.controllers.MesaListadoDTO;
import com.tulumcore.api.controllers.MesaLoteDTO;
import com.tulumcore.api.controllers.MesaTraspasoDTO;
import com.tulumcore.api.controllers.MesaDivisionDTO;
import com.tulumcore.api.controllers.VentaCobroDTO;
import com.tulumcore.api.controllers.VentaDTO;
import com.tulumcore.api.controllers.VentaListadoDTO;
import com.tulumcore.api.entities.FeatureKey;
import com.tulumcore.api.entities.Mesa;
import com.tulumcore.api.entities.Venta;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.repositories.MesaRepository;
import com.tulumcore.api.repositories.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MesaService {

    @Autowired private MesaRepository mesaRepository;
    @Autowired private VentaRepository ventaRepository;
    @Autowired private VentaService ventaService;
    @Autowired private TenantFeatureService tenantFeatureService;
    @Autowired private AuditoryLogService auditoryLogService;

    public List<MesaListadoDTO> listar() {
        tenantFeatureService.requireEnabled(FeatureKey.MESAS);
        String tenant = TenantContext.getCurrentTenant();
        List<Mesa> mesas = mesaRepository.findAllByTenantIdOrderByNumeroAsc(tenant);
        Map<Long, Venta> abiertas = mapearCuentasAbiertas(tenant, mesas);
        return mesas.stream()
                .map(m -> MesaListadoDTO.desde(m, abiertas.get(m.getId())))
                .collect(Collectors.toList());
    }

    public MesaListadoDTO obtener(Long id) {
        return obtenerCuenta(id).getMesa();
    }

    @Transactional(readOnly = true)
    public MesaCuentaDTO obtenerCuenta(Long id) {
        tenantFeatureService.requireEnabled(FeatureKey.MESAS);
        String tenant = TenantContext.getCurrentTenant();
        Mesa mesa = mesaRepository.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada: " + id));
        Venta abierta = ventaService.cuentaAbiertaDeMesa(id);
        VentaListadoDTO cuenta = abierta != null ? ventaService.toListadoConSaldo(abierta) : null;
        return MesaCuentaDTO.de(mesa, abierta, cuenta);
    }

    @Transactional
    public MesaCuentaDTO actualizarCuenta(Long id, VentaDTO dto) {
        tenantFeatureService.requireEnabled(FeatureKey.MESAS);
        ventaService.actualizarCuentaSalon(id, dto);
        return obtenerCuenta(id);
    }

    @Transactional
    public MesaCuentaDTO cobrar(Long id, VentaCobroDTO dto) {
        tenantFeatureService.requireEnabled(FeatureKey.MESAS);
        Venta cobrada = ventaService.cobrarCerrarMesa(id, dto);
        MesaCuentaDTO res = obtenerCuenta(id);
        if (res.getCuenta() == null) {
            res.setCuenta(ventaService.toListado(cobrada));
        }
        res.setParteCobrada(ventaService.toListado(cobrada));
        return res;
    }

    @Transactional
    public MesaCuentaDTO anularCuenta(Long id) {
        tenantFeatureService.requireEnabled(FeatureKey.MESAS);
        Venta abierta = ventaService.cuentaAbiertaDeMesa(id);
        if (abierta == null) {
            throw new BusinessException("Esa mesa no tiene una cuenta abierta.");
        }
        ventaService.anularVenta(abierta.getId());
        return obtenerCuenta(id);
    }

    @Transactional
    public MesaCuentaDTO pasar(Long id, MesaTraspasoDTO dto) {
        tenantFeatureService.requireEnabled(FeatureKey.MESAS);
        if (dto == null || dto.getMesaDestinoId() == null) {
            throw new BusinessException("Indicá la mesa destino.");
        }
        ventaService.pasarCuentaSalon(id, dto.getMesaDestinoId());
        return obtenerCuenta(dto.getMesaDestinoId());
    }

    @Transactional
    public MesaCuentaDTO juntar(Long id, MesaTraspasoDTO dto) {
        tenantFeatureService.requireEnabled(FeatureKey.MESAS);
        if (dto == null || dto.getMesaDestinoId() == null) {
            throw new BusinessException("Indicá la mesa destino.");
        }
        ventaService.juntarCuentasSalon(id, dto.getMesaDestinoId());
        return obtenerCuenta(dto.getMesaDestinoId());
    }

    @Transactional
    public MesaCuentaDTO dividir(Long id, MesaDivisionDTO dto) {
        tenantFeatureService.requireEnabled(FeatureKey.MESAS);
        Venta parte = ventaService.dividirCuentaSalon(id, dto);
        MesaCuentaDTO res = obtenerCuenta(id);
        res.setParteCobrada(ventaService.toListado(parte));
        return res;
    }

    @Transactional
    public MesaListadoDTO crear(MesaDTO dto) {
        tenantFeatureService.requireEnabled(FeatureKey.MESAS);
        String tenant = TenantContext.getCurrentTenant();
        validarNumero(dto.getNumero());
        if (mesaRepository.existsByTenantIdAndNumero(tenant, dto.getNumero())) {
            throw new BusinessException("Ya existe la mesa " + dto.getNumero() + ".");
        }
        Mesa mesa = new Mesa();
        mesa.setTenantId(tenant);
        mesa.setNumero(dto.getNumero());
        mesa.setNombre(textoOpcional(dto.getNombre()));
        mesa.setCapacidad(dto.getCapacidad());
        mesa.setActiva(dto.getActiva() == null || dto.getActiva());
        mesa.setEstado(Mesa.LIBRE);
        Mesa saved = mesaRepository.save(mesa);
        auditoryLogService.registrar("CREATE", "MESA", saved.getId(),
                "Mesa creada: " + saved.etiqueta(), null, detalle(saved));
        return MesaListadoDTO.desde(saved, null);
    }

    @Transactional
    public List<MesaListadoDTO> crearLote(MesaLoteDTO dto) {
        tenantFeatureService.requireEnabled(FeatureKey.MESAS);
        if (dto == null || dto.getDesde() == null || dto.getHasta() == null) {
            throw new BusinessException("Indicá el rango de mesas (desde / hasta).");
        }
        int desde = dto.getDesde();
        int hasta = dto.getHasta();
        if (desde < 1 || hasta < desde || hasta - desde > 80) {
            throw new BusinessException("Rango inválido. Usá desde ≥ 1 y hasta 80 mesas como máximo.");
        }
        List<MesaListadoDTO> creadas = new ArrayList<>();
        for (int n = desde; n <= hasta; n++) {
            if (mesaRepository.existsByTenantIdAndNumero(TenantContext.getCurrentTenant(), n)) {
                continue;
            }
            MesaDTO una = new MesaDTO();
            una.setNumero(n);
            creadas.add(crear(una));
        }
        return creadas;
    }

    @Transactional
    public MesaListadoDTO actualizar(Long id, MesaDTO dto) {
        tenantFeatureService.requireEnabled(FeatureKey.MESAS);
        String tenant = TenantContext.getCurrentTenant();
        Mesa mesa = mesaRepository.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada: " + id));
        String anterior = detalle(mesa);
        if (dto.getNumero() != null && !dto.getNumero().equals(mesa.getNumero())) {
            validarNumero(dto.getNumero());
            mesaRepository.findByTenantIdAndNumero(tenant, dto.getNumero()).ifPresent(otra -> {
                if (!otra.getId().equals(id)) {
                    throw new BusinessException("Ya existe la mesa " + dto.getNumero() + ".");
                }
            });
            mesa.setNumero(dto.getNumero());
        }
        if (dto.getNombre() != null) {
            mesa.setNombre(textoOpcional(dto.getNombre()));
        }
        if (dto.getCapacidad() != null) {
            mesa.setCapacidad(dto.getCapacidad());
        }
        if (dto.getActiva() != null) {
            if (!dto.getActiva() && Mesa.OCUPADA.equals(mesa.getEstado())) {
                throw new BusinessException("No se puede desactivar una mesa con cuenta abierta.");
            }
            mesa.setActiva(dto.getActiva());
        }
        Mesa saved = mesaRepository.save(mesa);
        auditoryLogService.registrar("UPDATE", "MESA", saved.getId(),
                "Mesa actualizada: " + saved.etiqueta(), anterior, detalle(saved));
        Venta abierta = ventaRepository.findCuentasAbiertasByMesa(tenant, id).stream().findFirst().orElse(null);
        return MesaListadoDTO.desde(saved, abierta);
    }

    @Transactional
    public void eliminar(Long id) {
        tenantFeatureService.requireEnabled(FeatureKey.MESAS);
        String tenant = TenantContext.getCurrentTenant();
        Mesa mesa = mesaRepository.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada: " + id));
        if (Mesa.OCUPADA.equals(mesa.getEstado())
                || !ventaRepository.findCuentasAbiertasByMesa(tenant, id).isEmpty()) {
            throw new BusinessException("No se puede eliminar una mesa con cuenta abierta. Cobrala o anulala antes.");
        }
        String anterior = detalle(mesa);
        ventaRepository.desvincularMesa(tenant, id);
        mesaRepository.delete(mesa);
        auditoryLogService.registrar("DELETE", "MESA", id,
                "Mesa eliminada: " + mesa.etiqueta(), anterior, null);
    }

    @Transactional
    public MesaListadoDTO abrirCuenta(Long id) {
        tenantFeatureService.requireEnabled(FeatureKey.MESAS);
        Venta venta = ventaService.abrirCuentaSalon(id);
        Mesa mesa = venta.getMesa();
        return MesaListadoDTO.desde(mesa, venta);
    }

    private Map<Long, Venta> mapearCuentasAbiertas(String tenant, List<Mesa> mesas) {
        if (mesas.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = mesas.stream().map(Mesa::getId).toList();
        Map<Long, Venta> map = new HashMap<>();
        for (Venta venta : ventaRepository.findCuentasAbiertasByMesas(tenant, ids)) {
            if (venta.getMesa() != null) {
                map.putIfAbsent(venta.getMesa().getId(), venta);
            }
        }
        return map;
    }

    private void validarNumero(Integer numero) {
        if (numero == null || numero < 1) {
            throw new BusinessException("El número de mesa debe ser mayor a cero.");
        }
    }

    private String textoOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    private String detalle(Mesa mesa) {
        return auditoryLogService.detalle(
                "numero", mesa.getNumero(),
                "nombre", mesa.getNombre(),
                "capacidad", mesa.getCapacidad(),
                "activa", mesa.isActiva(),
                "estado", mesa.getEstado()
        );
    }
}

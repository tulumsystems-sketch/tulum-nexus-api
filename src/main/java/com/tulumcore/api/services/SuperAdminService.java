package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.controllers.CreateTenantDTO;
import com.tulumcore.api.entities.Rol;
import com.tulumcore.api.entities.TenantConfig;
import com.tulumcore.api.entities.Usuario;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.repositories.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class SuperAdminService {

    private static final Set<String> TENANTS_RESERVADOS = Set.of(
            "superadmin", "admin", "api", "www", "login", "health", "tulum", "tulumcore", "tulum-core"
    );

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    public List<String> listarTenants() {
        return entityManager.createNativeQuery("""
                        SELECT tenant_id
                        FROM tenant_config
                        WHERE tenant_id IS NOT NULL
                        ORDER BY tenant_id
                        """, String.class)
                .getResultList();
    }

    public List<TenantConfig> listarConfigs() {
        List<TenantConfig> configs = new ArrayList<>();
        for (String tenantId : listarTenants()) {
            configs.add(getConfig(tenantId));
        }
        return configs;
    }

    @Transactional
    public TenantConfig crearTenant(CreateTenantDTO request) {
        String tenantId = normalizeTenantId(request.tenantId());

        if (!StringUtils.hasText(request.nombreEmpresa())) {
            throw new BusinessException("El nombre de empresa es obligatorio.");
        }
        if (!StringUtils.hasText(request.adminEmail())) {
            throw new BusinessException("El email admin es obligatorio.");
        }
        if (!StringUtils.hasText(request.adminPassword()) || request.adminPassword().length() < 8) {
            throw new BusinessException("La password admin debe tener al menos 8 caracteres.");
        }
        if (tenantExists(tenantId)) {
            throw new BusinessException("Ya existe un tenant con ese ID.");
        }
        if (emailExists(request.adminEmail())) {
            throw new BusinessException("Ese email ya existe. Usa otro email para el admin inicial.");
        }

        double iva = request.ivaPorcentaje() != null ? request.ivaPorcentaje() : 21.0;
        if (iva < 0 || iva > 27) {
            throw new BusinessException("El IVA debe estar entre 0 y 27.");
        }

        boolean pagoEfectivo = flag(request.pagoEfectivoHabilitado(), true);
        boolean pagoTransferencia = flag(request.pagoTransferenciaHabilitado(), false);
        boolean pagoMp = flag(request.pagoMercadoPagoHabilitado(), false);
        if (!pagoEfectivo && !pagoTransferencia && !pagoMp) {
            throw new BusinessException("El comercio necesita al menos un medio de pago.");
        }

        entityManager.createNativeQuery("""
                        INSERT INTO tenant_config (
                            tenant_id, nombre_empresa, activo,
                            clientes_habilitado, remitos_habilitado, compras_habilitado, stock_habilitado,
                            mp_aceptar_credito, mp_aceptar_debito, mp_aceptar_efectivo,
                            pago_efectivo_habilitado, pago_transferencia_habilitado, pago_mercado_pago_habilitado,
                            alias_cobro, iva_porcentaje, margen_por_defecto
                        )
                        VALUES (
                            :tenantId, :nombreEmpresa, true,
                            :clientes, :remitos, :compras, :stock,
                            true, true, true,
                            :pagoEfectivo, :pagoTransferencia, :pagoMp,
                            :aliasCobro, :iva, :margen
                        )
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("nombreEmpresa", request.nombreEmpresa().trim())
                .setParameter("clientes", flag(request.clientesHabilitado(), true))
                .setParameter("remitos", flag(request.remitosHabilitado(), true))
                .setParameter("compras", flag(request.comprasHabilitado(), true))
                .setParameter("stock", flag(request.stockHabilitado(), true))
                .setParameter("pagoEfectivo", pagoEfectivo)
                .setParameter("pagoTransferencia", pagoTransferencia)
                .setParameter("pagoMp", pagoMp)
                .setParameter("aliasCobro", blankToNull(request.aliasCobro()))
                .setParameter("iva", iva)
                .setParameter("margen", request.margenPorDefecto())
                .executeUpdate();

        entityManager.createNativeQuery("""
                        INSERT INTO usuarios (tenant_id, email, password, rol)
                        VALUES (:tenantId, :email, :password, :rol)
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("email", request.adminEmail().trim())
                .setParameter("password", passwordEncoder.encode(request.adminPassword()))
                .setParameter("rol", Rol.ADMIN.name())
                .executeUpdate();

        return getConfig(tenantId);
    }

    @Transactional
    public void toggleTenantStatus(String tenantId, boolean activo) {
        ensureConfigExists(tenantId);
        entityManager.createNativeQuery("""
                        UPDATE tenant_config
                        SET activo = :activo
                        WHERE tenant_id = :tenantId
                        """)
                .setParameter("activo", activo)
                .setParameter("tenantId", tenantId)
                .executeUpdate();
    }

    public TenantConfig getConfig(String tenantId) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT id, tenant_id, nombre_empresa, mp_access_token, logo_url,
                               clientes_habilitado, remitos_habilitado, compras_habilitado, stock_habilitado,
                               activo, mp_aceptar_credito, mp_aceptar_debito, mp_aceptar_efectivo,
                               pago_efectivo_habilitado, pago_transferencia_habilitado, pago_mercado_pago_habilitado,
                               alias_cobro, iva_porcentaje, margen_por_defecto
                        FROM tenant_config
                        WHERE tenant_id = :tenantId
                        LIMIT 1
                        """)
                .setParameter("tenantId", tenantId)
                .getResultList();

        if (rows.isEmpty()) {
            return defaultConfig(tenantId);
        }
        return mapTenantConfig(rows.get(0));
    }

    @Transactional
    public TenantConfig updateConfig(String tenantId, TenantConfig dto) {
        ensureConfigExists(tenantId);
        entityManager.createNativeQuery("""
                        UPDATE tenant_config
                        SET nombre_empresa = :nombreEmpresa,
                            clientes_habilitado = :clientesHabilitado,
                            remitos_habilitado = :remitosHabilitado,
                            compras_habilitado = :comprasHabilitado,
                            stock_habilitado = :stockHabilitado,
                            mp_aceptar_credito = :mpAceptarCredito,
                            mp_aceptar_debito = :mpAceptarDebito,
                            mp_aceptar_efectivo = :mpAceptarEfectivo,
                            mp_access_token = :mpAccessToken,
                            logo_url = :logoUrl,
                            pago_efectivo_habilitado = :pagoEfectivo,
                            pago_transferencia_habilitado = :pagoTransferencia,
                            pago_mercado_pago_habilitado = :pagoMp,
                            alias_cobro = :aliasCobro,
                            iva_porcentaje = :iva,
                            margen_por_defecto = :margen
                        WHERE tenant_id = :tenantId
                        """)
                .setParameter("nombreEmpresa", dto.getNombreEmpresa())
                .setParameter("clientesHabilitado", dto.isClientesHabilitado())
                .setParameter("remitosHabilitado", dto.isRemitosHabilitado())
                .setParameter("comprasHabilitado", dto.isComprasHabilitado())
                .setParameter("stockHabilitado", dto.isStockHabilitado())
                .setParameter("mpAceptarCredito", dto.isMpAceptarCredito())
                .setParameter("mpAceptarDebito", dto.isMpAceptarDebito())
                .setParameter("mpAceptarEfectivo", dto.isMpAceptarEfectivo())
                .setParameter("mpAccessToken", dto.getMpAccessToken())
                .setParameter("logoUrl", dto.getLogoUrl())
                .setParameter("pagoEfectivo", dto.isPagoEfectivoHabilitado())
                .setParameter("pagoTransferencia", dto.isPagoTransferenciaHabilitado())
                .setParameter("pagoMp", dto.isPagoMercadoPagoHabilitado())
                .setParameter("aliasCobro", dto.getAliasCobro())
                .setParameter("iva", dto.getIvaPorcentaje())
                .setParameter("margen", dto.getMargenPorDefecto())
                .setParameter("tenantId", tenantId)
                .executeUpdate();

        return getConfig(tenantId);
    }

    public List<Usuario> listarUsuarios(String tenantId) {
        String original = TenantContext.getCurrentTenant();
        try {
            TenantContext.setCurrentTenant(tenantId);
            return usuarioRepository.findAllByTenantId(tenantId);
        } finally {
            TenantContext.setCurrentTenant(original);
        }
    }

    private String normalizeTenantId(String rawTenantId) {
        if (!StringUtils.hasText(rawTenantId)) {
            throw new BusinessException("El tenant ID es obligatorio.");
        }
        String tenantId = rawTenantId.trim().toLowerCase();
        if (!tenantId.matches("[a-z0-9][a-z0-9_-]{2,40}")) {
            throw new BusinessException("El tenant ID debe tener 3 a 41 caracteres y usar letras, numeros, guion o guion bajo.");
        }
        if (TENANTS_RESERVADOS.contains(tenantId)) {
            throw new BusinessException("Ese identificador esta reservado.");
        }
        return tenantId;
    }

    private boolean tenantExists(String tenantId) {
        Number count = (Number) entityManager.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM (
                            SELECT tenant_id FROM tenant_config WHERE tenant_id = :tenantId
                            UNION
                            SELECT tenant_id FROM usuarios WHERE tenant_id = :tenantId
                        ) tenants
                        """)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        return count.longValue() > 0;
    }

    private boolean emailExists(String email) {
        Number count = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM usuarios WHERE email = :email")
                .setParameter("email", email.trim())
                .getSingleResult();
        return count.longValue() > 0;
    }

    private void ensureConfigExists(String tenantId) {
        Number count = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM tenant_config WHERE tenant_id = :tenantId")
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        if (count.longValue() > 0) {
            return;
        }

        entityManager.createNativeQuery("""
                        INSERT INTO tenant_config (
                            tenant_id, nombre_empresa, activo,
                            clientes_habilitado, remitos_habilitado, compras_habilitado, stock_habilitado,
                            mp_aceptar_credito, mp_aceptar_debito, mp_aceptar_efectivo,
                            pago_efectivo_habilitado, pago_transferencia_habilitado, pago_mercado_pago_habilitado,
                            iva_porcentaje
                        )
                        VALUES (
                            :tenantId, :tenantId, true,
                            true, true, true, true,
                            true, true, true,
                            true, false, false,
                            21
                        )
                        """)
                .setParameter("tenantId", tenantId)
                .executeUpdate();
    }

    private TenantConfig defaultConfig(String tenantId) {
        TenantConfig config = new TenantConfig();
        config.setTenantId(tenantId);
        config.setNombreEmpresa(tenantId);
        config.setClientesHabilitado(true);
        config.setRemitosHabilitado(true);
        config.setComprasHabilitado(true);
        config.setStockHabilitado(true);
        config.setMpAceptarCredito(true);
        config.setMpAceptarDebito(true);
        config.setMpAceptarEfectivo(true);
        config.setPagoEfectivoHabilitado(true);
        config.setPagoTransferenciaHabilitado(false);
        config.setPagoMercadoPagoHabilitado(false);
        config.setIvaPorcentaje(21.0);
        config.setActivo(true);
        return config;
    }

    private TenantConfig mapTenantConfig(Object[] row) {
        TenantConfig config = new TenantConfig();
        config.setId(((Number) row[0]).longValue());
        config.setTenantId((String) row[1]);
        config.setNombreEmpresa((String) row[2]);
        config.setMpAccessToken((String) row[3]);
        config.setLogoUrl((String) row[4]);
        config.setClientesHabilitado(asBool(row[5], true));
        config.setRemitosHabilitado(asBool(row[6], true));
        config.setComprasHabilitado(asBool(row[7], true));
        config.setStockHabilitado(asBool(row[8], true));
        config.setActivo(asBool(row[9], true));
        config.setMpAceptarCredito(asBool(row[10], true));
        config.setMpAceptarDebito(asBool(row[11], true));
        config.setMpAceptarEfectivo(asBool(row[12], true));
        config.setPagoEfectivoHabilitado(asBool(row[13], true));
        config.setPagoTransferenciaHabilitado(asBool(row[14], false));
        config.setPagoMercadoPagoHabilitado(asBool(row[15], false));
        config.setAliasCobro((String) row[16]);
        config.setIvaPorcentaje(asDouble(row[17], 21.0));
        config.setMargenPorDefecto(asNullableDouble(row[18]));
        return config;
    }

    private boolean flag(Boolean value, boolean defaultValue) {
        return value != null ? value : defaultValue;
    }

    private String blankToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean asBool(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private double asDouble(Object value, double defaultValue) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return defaultValue;
    }

    private Double asNullableDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }
}

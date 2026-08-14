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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class SuperAdminService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    public List<String> listarTenants() {
        Set<String> tenantIds = new LinkedHashSet<>();
        List<String> tables = entityManager.createNativeQuery("""
                        SELECT table_name
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND column_name = 'tenant_id'
                        ORDER BY table_name
                        """, String.class)
                .getResultList();

        for (String table : tables) {
            if (!table.matches("[a-zA-Z0-9_]+")) {
                continue;
            }
            List<String> values = entityManager.createNativeQuery(
                            "SELECT DISTINCT tenant_id FROM " + table + " WHERE tenant_id IS NOT NULL",
                            String.class)
                    .getResultList();
            tenantIds.addAll(values);
        }

        return tenantIds.stream().sorted().toList();
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
        if (!StringUtils.hasText(request.adminPassword()) || request.adminPassword().length() < 6) {
            throw new BusinessException("La password admin debe tener al menos 6 caracteres.");
        }
        if (tenantExists(tenantId)) {
            throw new BusinessException("Ya existe un tenant con ese ID.");
        }
        if (emailExists(request.adminEmail())) {
            throw new BusinessException("Ese email ya existe. Usa otro email para el admin inicial.");
        }

        entityManager.createNativeQuery("""
                        INSERT INTO tenant_config (
                            tenant_id, nombre_empresa, activo,
                            clientes_habilitado, remitos_habilitado, compras_habilitado, stock_habilitado,
                            mp_aceptar_credito, mp_aceptar_debito, mp_aceptar_efectivo
                        )
                        VALUES (
                            :tenantId, :nombreEmpresa, true,
                            true, true, true, true,
                            true, true, true
                        )
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("nombreEmpresa", request.nombreEmpresa().trim())
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
                               activo, mp_aceptar_credito, mp_aceptar_debito, mp_aceptar_efectivo
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
                            logo_url = :logoUrl
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
                            mp_aceptar_credito, mp_aceptar_debito, mp_aceptar_efectivo
                        )
                        VALUES (
                            :tenantId, :tenantId, true,
                            true, true, true, true,
                            true, true, true
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
        config.setClientesHabilitado(Boolean.TRUE.equals(row[5]));
        config.setRemitosHabilitado(Boolean.TRUE.equals(row[6]));
        config.setComprasHabilitado(Boolean.TRUE.equals(row[7]));
        config.setStockHabilitado(Boolean.TRUE.equals(row[8]));
        config.setActivo(Boolean.TRUE.equals(row[9]));
        config.setMpAceptarCredito(Boolean.TRUE.equals(row[10]));
        config.setMpAceptarDebito(Boolean.TRUE.equals(row[11]));
        config.setMpAceptarEfectivo(Boolean.TRUE.equals(row[12]));
        return config;
    }
}

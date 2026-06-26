package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.TenantConfig;
import com.tulumcore.api.entities.Usuario;
import com.tulumcore.api.repositories.TenantConfigRepository;
import com.tulumcore.api.repositories.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SuperAdminService {

    @Autowired
    private TenantConfigRepository configRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public List<String> listarTenants() {
        return entityManager.createNativeQuery(
                "SELECT DISTINCT tenant_id FROM tenant_config ORDER BY tenant_id", String.class)
                .getResultList();
    }

    public List<TenantConfig> listarConfigs() {
        return entityManager.createNativeQuery(
                "SELECT * FROM tenant_config ORDER BY tenant_id", TenantConfig.class)
                .getResultList();
    }

    @Transactional
    public void toggleTenantStatus(String tenantId, boolean activo) {
        entityManager.createNativeQuery(
                "UPDATE tenant_config SET activo = :activo WHERE tenant_id = :tenantId")
                .setParameter("activo", activo)
                .setParameter("tenantId", tenantId)
                .executeUpdate();
    }

    public TenantConfig getConfig(String tenantId) {
        String original = TenantContext.getCurrentTenant();
        try {
            TenantContext.setCurrentTenant(tenantId);
            return configRepository.findByTenantId(tenantId).orElse(new TenantConfig());
        } finally {
            TenantContext.setCurrentTenant(original);
        }
    }

    @Transactional
    public TenantConfig updateConfig(String tenantId, TenantConfig dto) {
        String original = TenantContext.getCurrentTenant();
        try {
            TenantContext.setCurrentTenant(tenantId);

            TenantConfig config = configRepository.findByTenantId(tenantId)
                    .orElseGet(() -> {
                        TenantConfig c = new TenantConfig();
                        c.setTenantId(tenantId);
                        return c;
                    });

            config.setNombreEmpresa(dto.getNombreEmpresa());
            config.setClientesHabilitado(dto.isClientesHabilitado());
            config.setRemitosHabilitado(dto.isRemitosHabilitado());
            config.setComprasHabilitado(dto.isComprasHabilitado());
            config.setStockHabilitado(dto.isStockHabilitado());
            config.setMpAceptarCredito(dto.isMpAceptarCredito());
            config.setMpAceptarDebito(dto.isMpAceptarDebito());
            config.setMpAceptarEfectivo(dto.isMpAceptarEfectivo());
            config.setMpAccessToken(dto.getMpAccessToken());
            config.setLogoUrl(dto.getLogoUrl());

            return configRepository.save(config);
        } finally {
            TenantContext.setCurrentTenant(original);
        }
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
}

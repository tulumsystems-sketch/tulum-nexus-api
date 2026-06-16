package com.tulumcore.api.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "tenant_config")
public class TenantConfig extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreEmpresa;
    private String mpAccessToken;
    private String logoUrl;

    // --- VISIBILIDAD DE MÓDULOS ---
    @Column(columnDefinition = "boolean default true")
    private boolean clientesHabilitado = true;
    @Column(columnDefinition = "boolean default true")
    private boolean remitosHabilitado = true;

    // --- CONFIGURACIÓN DE MEDIOS DE PAGO ---
    private boolean mpAceptarCredito = false;
    private boolean mpAceptarDebito = false;
    private boolean mpAceptarEfectivo = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombreEmpresa() { return nombreEmpresa; }
    public void setNombreEmpresa(String nombreEmpresa) { this.nombreEmpresa = nombreEmpresa; }
    public String getMpAccessToken() { return mpAccessToken; }
    public void setMpAccessToken(String mpAccessToken) { this.mpAccessToken = mpAccessToken; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public boolean isMpAceptarCredito() { return mpAceptarCredito; }
    public void setMpAceptarCredito(boolean mpAceptarCredito) { this.mpAceptarCredito = mpAceptarCredito; }
    public boolean isMpAceptarDebito() { return mpAceptarDebito; }
    public void setMpAceptarDebito(boolean mpAceptarDebito) { this.mpAceptarDebito = mpAceptarDebito; }
    public boolean isMpAceptarEfectivo() { return mpAceptarEfectivo; }
    public void setMpAceptarEfectivo(boolean mpAceptarEfectivo) { this.mpAceptarEfectivo = mpAceptarEfectivo; }
    public boolean isClientesHabilitado() { return clientesHabilitado; }
    public void setClientesHabilitado(boolean clientesHabilitado) { this.clientesHabilitado = clientesHabilitado; }
    public boolean isRemitosHabilitado() { return remitosHabilitado; }
    public void setRemitosHabilitado(boolean remitosHabilitado) { this.remitosHabilitado = remitosHabilitado; }
}
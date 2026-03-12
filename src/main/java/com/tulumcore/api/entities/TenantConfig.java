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

    // --- CONFIGURACIÓN DE MEDIOS DE PAGO ---
    private boolean mpAceptarCredito = true;
    private boolean mpAceptarDebito = true;
    private boolean mpAceptarEfectivo = false; // Por defecto falso para evitar ventas colgadas por días

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
}
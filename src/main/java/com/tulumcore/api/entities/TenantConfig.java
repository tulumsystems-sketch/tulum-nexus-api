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
    @Column(columnDefinition = "boolean default true")
    private boolean comprasHabilitado = true;
    @Column(columnDefinition = "boolean default true")
    private boolean stockHabilitado = true;

    // --- ESTADO ---
    @Column(columnDefinition = "boolean default true")
    private boolean activo = true;

    // --- CONFIGURACIÓN DE MEDIOS DE PAGO ---
    @Column(columnDefinition = "boolean default true")
    private boolean mpAceptarCredito = true;
    @Column(columnDefinition = "boolean default true")
    private boolean mpAceptarDebito = true;
    @Column(columnDefinition = "boolean default true")
    private boolean mpAceptarEfectivo = true;

    // --- MÉTODOS DE PAGO HABILITADOS EN EL PUNTO DE VENTA ---
    private boolean pagoEfectivoHabilitado = true;
    private boolean pagoTransferenciaHabilitado = false;
    private boolean pagoMercadoPagoHabilitado = true;

    /** Alias / CBU que se imprime en el ticket cuando se cobra por transferencia. */
    private String aliasCobro;

    // --- POLÍTICA FISCAL Y DE PRECIOS ---
    /** Porcentaje de IVA aplicado a las ventas. 0 = no se discrimina IVA. */
    private double ivaPorcentaje = 21.0;

    /** Margen por defecto sobre el precio de costo. null = se carga el precio de venta a mano. */
    private Double margenPorDefecto;

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
    public boolean isComprasHabilitado() { return comprasHabilitado; }
    public void setComprasHabilitado(boolean comprasHabilitado) { this.comprasHabilitado = comprasHabilitado; }
    public boolean isStockHabilitado() { return stockHabilitado; }
    public void setStockHabilitado(boolean stockHabilitado) { this.stockHabilitado = stockHabilitado; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public boolean isPagoEfectivoHabilitado() { return pagoEfectivoHabilitado; }
    public void setPagoEfectivoHabilitado(boolean pagoEfectivoHabilitado) { this.pagoEfectivoHabilitado = pagoEfectivoHabilitado; }
    public boolean isPagoTransferenciaHabilitado() { return pagoTransferenciaHabilitado; }
    public void setPagoTransferenciaHabilitado(boolean pagoTransferenciaHabilitado) { this.pagoTransferenciaHabilitado = pagoTransferenciaHabilitado; }
    public boolean isPagoMercadoPagoHabilitado() { return pagoMercadoPagoHabilitado; }
    public void setPagoMercadoPagoHabilitado(boolean pagoMercadoPagoHabilitado) { this.pagoMercadoPagoHabilitado = pagoMercadoPagoHabilitado; }
    public String getAliasCobro() { return aliasCobro; }
    public void setAliasCobro(String aliasCobro) { this.aliasCobro = aliasCobro; }
    public double getIvaPorcentaje() { return ivaPorcentaje; }
    public void setIvaPorcentaje(double ivaPorcentaje) { this.ivaPorcentaje = ivaPorcentaje; }
    public Double getMargenPorDefecto() { return margenPorDefecto; }
    public void setMargenPorDefecto(Double margenPorDefecto) { this.margenPorDefecto = margenPorDefecto; }
}
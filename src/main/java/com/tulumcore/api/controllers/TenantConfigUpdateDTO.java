package com.tulumcore.api.controllers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload de actualización de configuración del tenant.
 *
 * Clase (no record) a propósito: Jackson deserializa bien los booleanos en false
 * y el IVA en 0, que en un record sin -parameters pueden llegar siempre como null
 * y el merge parcial no aplica ningún cambio.
 *
 * null = no tocar ese campo.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TenantConfigUpdateDTO {

    @JsonProperty("nombreEmpresa")
    private String nombreEmpresa;
    @JsonProperty("logoUrl")
    private String logoUrl;
    @JsonProperty("mpAccessToken")
    private String mpAccessToken;
    @JsonProperty("mpAceptarCredito")
    private Boolean mpAceptarCredito;
    @JsonProperty("mpAceptarDebito")
    private Boolean mpAceptarDebito;
    @JsonProperty("mpAceptarEfectivo")
    private Boolean mpAceptarEfectivo;
    @JsonProperty("clientesHabilitado")
    private Boolean clientesHabilitado;
    @JsonProperty("remitosHabilitado")
    private Boolean remitosHabilitado;
    @JsonProperty("comprasHabilitado")
    private Boolean comprasHabilitado;
    @JsonProperty("stockHabilitado")
    private Boolean stockHabilitado;
    @JsonProperty("pagoEfectivoHabilitado")
    private Boolean pagoEfectivoHabilitado;
    @JsonProperty("pagoTransferenciaHabilitado")
    private Boolean pagoTransferenciaHabilitado;
    @JsonProperty("pagoMercadoPagoHabilitado")
    private Boolean pagoMercadoPagoHabilitado;
    @JsonProperty("aliasCobro")
    private String aliasCobro;
    @JsonProperty("ivaPorcentaje")
    private Double ivaPorcentaje;
    @JsonProperty("margenPorDefecto")
    private Double margenPorDefecto;
    @JsonProperty("limpiarMargenPorDefecto")
    private Boolean limpiarMargenPorDefecto;

    public String getNombreEmpresa() { return nombreEmpresa; }
    public void setNombreEmpresa(String nombreEmpresa) { this.nombreEmpresa = nombreEmpresa; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getMpAccessToken() { return mpAccessToken; }
    public void setMpAccessToken(String mpAccessToken) { this.mpAccessToken = mpAccessToken; }
    public Boolean getMpAceptarCredito() { return mpAceptarCredito; }
    public void setMpAceptarCredito(Boolean mpAceptarCredito) { this.mpAceptarCredito = mpAceptarCredito; }
    public Boolean getMpAceptarDebito() { return mpAceptarDebito; }
    public void setMpAceptarDebito(Boolean mpAceptarDebito) { this.mpAceptarDebito = mpAceptarDebito; }
    public Boolean getMpAceptarEfectivo() { return mpAceptarEfectivo; }
    public void setMpAceptarEfectivo(Boolean mpAceptarEfectivo) { this.mpAceptarEfectivo = mpAceptarEfectivo; }
    public Boolean getClientesHabilitado() { return clientesHabilitado; }
    public void setClientesHabilitado(Boolean clientesHabilitado) { this.clientesHabilitado = clientesHabilitado; }
    public Boolean getRemitosHabilitado() { return remitosHabilitado; }
    public void setRemitosHabilitado(Boolean remitosHabilitado) { this.remitosHabilitado = remitosHabilitado; }
    public Boolean getComprasHabilitado() { return comprasHabilitado; }
    public void setComprasHabilitado(Boolean comprasHabilitado) { this.comprasHabilitado = comprasHabilitado; }
    public Boolean getStockHabilitado() { return stockHabilitado; }
    public void setStockHabilitado(Boolean stockHabilitado) { this.stockHabilitado = stockHabilitado; }
    public Boolean getPagoEfectivoHabilitado() { return pagoEfectivoHabilitado; }
    public void setPagoEfectivoHabilitado(Boolean pagoEfectivoHabilitado) { this.pagoEfectivoHabilitado = pagoEfectivoHabilitado; }
    public Boolean getPagoTransferenciaHabilitado() { return pagoTransferenciaHabilitado; }
    public void setPagoTransferenciaHabilitado(Boolean pagoTransferenciaHabilitado) { this.pagoTransferenciaHabilitado = pagoTransferenciaHabilitado; }
    public Boolean getPagoMercadoPagoHabilitado() { return pagoMercadoPagoHabilitado; }
    public void setPagoMercadoPagoHabilitado(Boolean pagoMercadoPagoHabilitado) { this.pagoMercadoPagoHabilitado = pagoMercadoPagoHabilitado; }
    public String getAliasCobro() { return aliasCobro; }
    public void setAliasCobro(String aliasCobro) { this.aliasCobro = aliasCobro; }
    public Double getIvaPorcentaje() { return ivaPorcentaje; }
    public void setIvaPorcentaje(Double ivaPorcentaje) { this.ivaPorcentaje = ivaPorcentaje; }
    public Double getMargenPorDefecto() { return margenPorDefecto; }
    public void setMargenPorDefecto(Double margenPorDefecto) { this.margenPorDefecto = margenPorDefecto; }
    public Boolean getLimpiarMargenPorDefecto() { return limpiarMargenPorDefecto; }
    public void setLimpiarMargenPorDefecto(Boolean limpiarMargenPorDefecto) { this.limpiarMargenPorDefecto = limpiarMargenPorDefecto; }
}

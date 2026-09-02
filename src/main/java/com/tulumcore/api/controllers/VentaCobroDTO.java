package com.tulumcore.api.controllers;

public class VentaCobroDTO {
    private Boolean cobrado;
    private String metodoPago;
    private Double montoAbonado;

    public Boolean getCobrado() { return cobrado; }
    public void setCobrado(Boolean cobrado) { this.cobrado = cobrado; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public Double getMontoAbonado() { return montoAbonado; }
    public void setMontoAbonado(Double montoAbonado) { this.montoAbonado = montoAbonado; }
}

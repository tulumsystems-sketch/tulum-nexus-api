package com.tulumcore.api.controllers;

public class BotCadeteAccionDTO {
    private String telefono;
    private Long ventaId;
    private Boolean cobrado;
    private String metodoPago;
    private String estado;

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public Long getVentaId() { return ventaId; }
    public void setVentaId(Long ventaId) { this.ventaId = ventaId; }
    public Boolean getCobrado() { return cobrado; }
    public void setCobrado(Boolean cobrado) { this.cobrado = cobrado; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}

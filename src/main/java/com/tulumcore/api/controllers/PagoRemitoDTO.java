package com.tulumcore.api.controllers;

// DTO de entrada para registrar una cobranza sobre un remito
public class PagoRemitoDTO {
    private Double monto;
    private String metodoPago; // EFECTIVO, TRANSFERENCIA, MERCADO_PAGO
    private String observaciones;

    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}

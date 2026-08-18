package com.tulumcore.api.controllers;

// Totales de cobranzas de remitos para el tablero de cuentas por cobrar
public class ResumenCobranzasDTO {

    private long cantidadRemitos;
    private long cantidadImpagos;
    private long cantidadParciales;
    private long cantidadPagados;
    private double totalFacturado;
    private double totalCobrado;
    private double totalPendiente;

    public long getCantidadRemitos() { return cantidadRemitos; }
    public void setCantidadRemitos(long cantidadRemitos) { this.cantidadRemitos = cantidadRemitos; }
    public long getCantidadImpagos() { return cantidadImpagos; }
    public void setCantidadImpagos(long cantidadImpagos) { this.cantidadImpagos = cantidadImpagos; }
    public long getCantidadParciales() { return cantidadParciales; }
    public void setCantidadParciales(long cantidadParciales) { this.cantidadParciales = cantidadParciales; }
    public long getCantidadPagados() { return cantidadPagados; }
    public void setCantidadPagados(long cantidadPagados) { this.cantidadPagados = cantidadPagados; }
    public double getTotalFacturado() { return totalFacturado; }
    public void setTotalFacturado(double totalFacturado) { this.totalFacturado = totalFacturado; }
    public double getTotalCobrado() { return totalCobrado; }
    public void setTotalCobrado(double totalCobrado) { this.totalCobrado = totalCobrado; }
    public double getTotalPendiente() { return totalPendiente; }
    public void setTotalPendiente(double totalPendiente) { this.totalPendiente = totalPendiente; }
}

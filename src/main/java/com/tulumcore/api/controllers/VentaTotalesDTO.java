package com.tulumcore.api.controllers;

public class VentaTotalesDTO {
    private long cantidad;
    private double ingresos;
    private double ticketPromedio;

    public VentaTotalesDTO() {}

    public VentaTotalesDTO(long cantidad, double ingresos) {
        this.cantidad = cantidad;
        this.ingresos = ingresos;
        this.ticketPromedio = cantidad > 0 ? ingresos / cantidad : 0;
    }

    public long getCantidad() { return cantidad; }
    public void setCantidad(long cantidad) { this.cantidad = cantidad; }
    public double getIngresos() { return ingresos; }
    public void setIngresos(double ingresos) { this.ingresos = ingresos; }
    public double getTicketPromedio() { return ticketPromedio; }
    public void setTicketPromedio(double ticketPromedio) { this.ticketPromedio = ticketPromedio; }
}

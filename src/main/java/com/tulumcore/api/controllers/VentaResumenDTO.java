package com.tulumcore.api.controllers;

import java.time.LocalDate;

public class VentaResumenDTO {
    private LocalDate fecha;
    private Double efectivo;
    private Double mercadoPago;

    public VentaResumenDTO(LocalDate fecha, Double efectivo, Double mercadoPago) {
        this.fecha = fecha;
        this.efectivo = efectivo;
        this.mercadoPago = mercadoPago;
    }

    // Getters y Setters
    public LocalDate getFecha() { return fecha; }
    public Double getEfectivo() { return efectivo; }
    public Double getMercadoPago() { return mercadoPago; }
}
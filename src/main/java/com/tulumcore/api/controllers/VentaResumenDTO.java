package com.tulumcore.api.controllers;

import java.time.LocalDate;

public class VentaResumenDTO {
    private LocalDate fecha;
    private Double efectivo;
    private Double mercadoPago;
    private Double transferencia;

    public VentaResumenDTO(LocalDate fecha, Double efectivo, Double mercadoPago) {
        this(fecha, efectivo, mercadoPago, 0.0);
    }

    public VentaResumenDTO(LocalDate fecha, Double efectivo, Double mercadoPago, Double transferencia) {
        this.fecha = fecha;
        this.efectivo = efectivo;
        this.mercadoPago = mercadoPago;
        this.transferencia = transferencia;
    }

    // Getters y Setters
    public LocalDate getFecha() { return fecha; }
    public Double getEfectivo() { return efectivo; }
    public Double getMercadoPago() { return mercadoPago; }
    public Double getTransferencia() { return transferencia; }
}

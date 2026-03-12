package com.tulumcore.api.controllers;

import java.util.List;

public class VentaDTO {
    public Long clienteId;
    public String observaciones;
    public List<ItemVentaDTO> items;

    // --- NUEVOS CAMPOS ---
    public String metodoPago; // "MERCADO_PAGO" o "EFECTIVO"
    public Double montoAbonado;
}
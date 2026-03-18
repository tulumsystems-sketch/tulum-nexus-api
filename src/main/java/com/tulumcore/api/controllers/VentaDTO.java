package com.tulumcore.api.controllers;

import java.util.List;

public class VentaDTO {
    public Long clienteId;
    public String metodoPago;
    public String observaciones;
    public Double montoAbonado;
    public List<ItemVentaDTO> items;

    // --- Getters y Setters ---
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public Double getMontoAbonado() { return montoAbonado; }
    public void setMontoAbonado(Double montoAbonado) { this.montoAbonado = montoAbonado; }

    public List<ItemVentaDTO> getItems() { return items; }
    public void setItems(List<ItemVentaDTO> items) { this.items = items; }
}
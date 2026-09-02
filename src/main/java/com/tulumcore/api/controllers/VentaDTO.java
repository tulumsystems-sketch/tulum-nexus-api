package com.tulumcore.api.controllers;

import java.util.List;

public class VentaDTO {
    public Long clienteId;
    public String metodoPago;
    public String observaciones;
    public Double montoAbonado;
    public String canal;
    public String nombreContacto;
    public String telefonoContacto;
    public String direccionEntrega;
    public Boolean cobrado;
    public Long mesaId;
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

    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }

    public String getNombreContacto() { return nombreContacto; }
    public void setNombreContacto(String nombreContacto) { this.nombreContacto = nombreContacto; }

    public String getTelefonoContacto() { return telefonoContacto; }
    public void setTelefonoContacto(String telefonoContacto) { this.telefonoContacto = telefonoContacto; }

    public String getDireccionEntrega() { return direccionEntrega; }
    public void setDireccionEntrega(String direccionEntrega) { this.direccionEntrega = direccionEntrega; }

    public Boolean getCobrado() { return cobrado; }
    public void setCobrado(Boolean cobrado) { this.cobrado = cobrado; }

    public Long getMesaId() { return mesaId; }
    public void setMesaId(Long mesaId) { this.mesaId = mesaId; }

    public List<ItemVentaDTO> getItems() { return items; }
    public void setItems(List<ItemVentaDTO> items) { this.items = items; }
}
package com.tulumcore.api.controllers;

import java.util.List;

// DTO de entrada para crear un remito
public class RemitoDTO {
    private Long clienteId;
    private String direccionEntrega;
    private String nombreDestinatario;
    private String telefonoDestinatario;
    private String observaciones;
    private List<ItemRemitoDTO> items;

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public String getDireccionEntrega() { return direccionEntrega; }
    public void setDireccionEntrega(String direccionEntrega) { this.direccionEntrega = direccionEntrega; }
    public String getNombreDestinatario() { return nombreDestinatario; }
    public void setNombreDestinatario(String nombreDestinatario) { this.nombreDestinatario = nombreDestinatario; }
    public String getTelefonoDestinatario() { return telefonoDestinatario; }
    public void setTelefonoDestinatario(String telefonoDestinatario) { this.telefonoDestinatario = telefonoDestinatario; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public List<ItemRemitoDTO> getItems() { return items; }
    public void setItems(List<ItemRemitoDTO> items) { this.items = items; }
}
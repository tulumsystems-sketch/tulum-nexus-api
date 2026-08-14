package com.tulumcore.api.controllers;

import java.util.List;

public class CompraDTO {

    private Long proveedorId;
    private String nroFactura;
    private String observaciones;
    private List<ItemCompraDTO> items;

    public Long getProveedorId() { return proveedorId; }
    public void setProveedorId(Long proveedorId) { this.proveedorId = proveedorId; }
    public String getNroFactura() { return nroFactura; }
    public void setNroFactura(String nroFactura) { this.nroFactura = nroFactura; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public List<ItemCompraDTO> getItems() { return items; }
    public void setItems(List<ItemCompraDTO> items) { this.items = items; }
}

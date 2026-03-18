package com.tulumcore.api.controllers;

public class ItemVentaDTO {
    public Long productoId;
    public Integer cantidad;

    // --- Getters y Setters Manuales ---
    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
}
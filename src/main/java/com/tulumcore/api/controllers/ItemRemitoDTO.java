package com.tulumcore.api.controllers;

public class ItemRemitoDTO {
    private Long productoId;      // Puede ser null si es un ítem libre
    private Integer cantidad;
    private String descripcion;   // Descripción libre si no hay producto

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
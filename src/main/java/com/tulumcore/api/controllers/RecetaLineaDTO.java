package com.tulumcore.api.controllers;

public class RecetaLineaDTO {
    private Long insumoId;
    private String insumoNombre;
    private Double cantidad;
    private String unidad;

    public RecetaLineaDTO() {}

    public RecetaLineaDTO(Long insumoId, String insumoNombre, Double cantidad, String unidad) {
        this.insumoId = insumoId;
        this.insumoNombre = insumoNombre;
        this.cantidad = cantidad;
        this.unidad = unidad;
    }

    public Long getInsumoId() { return insumoId; }
    public void setInsumoId(Long insumoId) { this.insumoId = insumoId; }
    public String getInsumoNombre() { return insumoNombre; }
    public void setInsumoNombre(String insumoNombre) { this.insumoNombre = insumoNombre; }
    public Double getCantidad() { return cantidad; }
    public void setCantidad(Double cantidad) { this.cantidad = cantidad; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
}

package com.tulumcore.api.controllers;

public class ProductoDTO {
    private String nombre;
    private String descripcion;
    private Double precio;
    private Double precioCosto;
    private Double margenPorcentaje;
    private Double cantidadStock;
    private Integer stockMinimo;
    private String medidas;
    private String codigoBarras;
    private Long categoriaId;
    private String imageUrl;
    private Boolean publicadoEnCatalogo;
    private String tipo;
    private Boolean vendible;
    private java.util.List<RecetaLineaDTO> receta;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Double getPrecio() { return precio; }
    public void setPrecio(Double precio) { this.precio = precio; }
    public Double getPrecioCosto() { return precioCosto; }
    public void setPrecioCosto(Double precioCosto) { this.precioCosto = precioCosto; }
    public Double getMargenPorcentaje() { return margenPorcentaje; }
    public void setMargenPorcentaje(Double margenPorcentaje) { this.margenPorcentaje = margenPorcentaje; }
    public Double getCantidadStock() { return cantidadStock; }
    public void setCantidadStock(Double cantidadStock) { this.cantidadStock = cantidadStock; }
    public Integer getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(Integer stockMinimo) { this.stockMinimo = stockMinimo; }
    public String getMedidas() { return medidas; }
    public void setMedidas(String medidas) { this.medidas = medidas; }
    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }
    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Boolean getPublicadoEnCatalogo() { return publicadoEnCatalogo; }
    public void setPublicadoEnCatalogo(Boolean publicadoEnCatalogo) { this.publicadoEnCatalogo = publicadoEnCatalogo; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Boolean getVendible() { return vendible; }
    public void setVendible(Boolean vendible) { this.vendible = vendible; }
    public java.util.List<RecetaLineaDTO> getReceta() { return receta; }
    public void setReceta(java.util.List<RecetaLineaDTO> receta) { this.receta = receta; }
}

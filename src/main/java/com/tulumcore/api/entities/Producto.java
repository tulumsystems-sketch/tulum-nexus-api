package com.tulumcore.api.entities;

import jakarta.persistence.*;

@Entity
public class Producto extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String descripcion;
    private Double precio;
    private Integer cantidadStock;
    private Integer stockMinimo = 0; // Alerta cuando cantidadStock <= stockMinimo
    private String medidas;

    @Column(name = "codigo_barras", length = 64)
    private String codigoBarras;

    @Column(name = "image_url")
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Double getPrecio() { return precio; }
    public void setPrecio(Double precio) { this.precio = precio; }
    public Integer getCantidadStock() { return cantidadStock; }
    public void setCantidadStock(Integer cantidadStock) { this.cantidadStock = cantidadStock; }
    public Integer getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(Integer stockMinimo) { this.stockMinimo = stockMinimo; }
    public String getMedidas() { return medidas; }
    public void setMedidas(String medidas) { this.medidas = medidas; }
    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
}

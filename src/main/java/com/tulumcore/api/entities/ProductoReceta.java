package com.tulumcore.api.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "producto_receta")
public class ProductoReceta extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Producto insumo;

    @Column(nullable = false)
    private Double cantidad;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public Producto getInsumo() { return insumo; }
    public void setInsumo(Producto insumo) { this.insumo = insumo; }
    public Double getCantidad() { return cantidad; }
    public void setCantidad(Double cantidad) { this.cantidad = cantidad; }
}

package com.tulumcore.api.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "compra_items")
public class ItemCompra extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "compra_id")
    private Compra compra;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private Integer cantidad;
    private Double precioUnitario;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Compra getCompra() { return compra; }
    public void setCompra(Compra compra) { this.compra = compra; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public Double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(Double precioUnitario) { this.precioUnitario = precioUnitario; }
}

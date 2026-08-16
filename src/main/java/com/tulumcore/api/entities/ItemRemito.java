package com.tulumcore.api.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "remito_items")
public class ItemRemito extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "remito_id")
    private Remito remito;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private Integer cantidad;
    private Double precioUnitario = 0.0;
    private Double totalLinea = 0.0;
    private String descripcion; // Para ítems sin producto registrado (servicios, etc.)

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Remito getRemito() { return remito; }
    public void setRemito(Remito remito) { this.remito = remito; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(Double precioUnitario) { this.precioUnitario = precioUnitario; }
    public Double getTotalLinea() { return totalLinea; }
    public void setTotalLinea(Double totalLinea) { this.totalLinea = totalLinea; }
}

package com.tulumcore.api.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "remitos")
public class Remito extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nroRemito;
    private LocalDateTime fecha = LocalDateTime.now();
    private String estado = "PENDIENTE"; // PENDIENTE, EN_VIAJE, ENTREGADO, INCIDENCIA

    private String direccionEntrega;
    private String nombreDestinatario;
    private String telefonoDestinatario;
    private String observaciones;
    private Double total = 0.0;

    /** Estado de cobranza del remito: IMPAGO, PAGADO_PARCIAL o PAGADO. */
    @Column(name = "estado_pago")
    private String estadoPago = "IMPAGO";

    @Column(name = "monto_pagado")
    private Double montoPagado = 0.0;

    @Column(name = "saldo_pendiente")
    private Double saldoPendiente = 0.0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @JsonManagedReference
    @OneToMany(mappedBy = "remito", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ItemRemito> items;

    // --- Getters y Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNroRemito() { return nroRemito; }
    public void setNroRemito(String nroRemito) { this.nroRemito = nroRemito; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getDireccionEntrega() { return direccionEntrega; }
    public void setDireccionEntrega(String direccionEntrega) { this.direccionEntrega = direccionEntrega; }
    public String getNombreDestinatario() { return nombreDestinatario; }
    public void setNombreDestinatario(String nombreDestinatario) { this.nombreDestinatario = nombreDestinatario; }
    public String getTelefonoDestinatario() { return telefonoDestinatario; }
    public void setTelefonoDestinatario(String telefonoDestinatario) { this.telefonoDestinatario = telefonoDestinatario; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }
    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }
    public Double getMontoPagado() { return montoPagado; }
    public void setMontoPagado(Double montoPagado) { this.montoPagado = montoPagado; }
    public Double getSaldoPendiente() { return saldoPendiente; }
    public void setSaldoPendiente(Double saldoPendiente) { this.saldoPendiente = saldoPendiente; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public List<ItemRemito> getItems() { return items; }
    public void setItems(List<ItemRemito> items) { this.items = items; }
}

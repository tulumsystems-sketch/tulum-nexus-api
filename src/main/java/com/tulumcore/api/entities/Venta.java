package com.tulumcore.api.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ventas")
public class Venta extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fecha = LocalDateTime.now();
    private String nroComprobante;
    private String observaciones;
    private String estado = "PENDIENTE";
    /** MOSTRADOR, WHATSAPP, DELIVERY, RETIRO o SALON. Default mostrador para no romper ventas viejas. */
    private String canal = "MOSTRADOR";
    private String nombreContacto;
    private String telefonoContacto;
    private String direccionEntrega;
    /** Cadete que tomó el envío (autoasignación). Null = sigue en cola de salida. */
    private String repartidorNombre;
    private Long repartidorUsuarioId;
    /** Independiente del estado de cocina: el pedido puede estar en prep y ya cobrado. */
    private boolean cobrado = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mesa_id")
    private Mesa mesa;
    private Long ventaOrigenId;
    private Double totalNeto;
    private Double totalIva;
    private Double totalFinal;
    private String moneda = "ARS";

    // --- NUEVOS CAMPOS CAJA FÍSICA ---
    private String metodoPago = "MERCADO_PAGO"; // Puede ser MERCADO_PAGO o EFECTIVO
    private Double montoAbonado = 0.0;
    private Double vuelto = 0.0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @JsonManagedReference
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ItemVenta> items;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getNroComprobante() { return nroComprobante; }
    public void setNroComprobante(String nroComprobante) { this.nroComprobante = nroComprobante; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }
    public String getNombreContacto() { return nombreContacto; }
    public void setNombreContacto(String nombreContacto) { this.nombreContacto = nombreContacto; }
    public String getTelefonoContacto() { return telefonoContacto; }
    public void setTelefonoContacto(String telefonoContacto) { this.telefonoContacto = telefonoContacto; }
    public String getDireccionEntrega() { return direccionEntrega; }
    public void setDireccionEntrega(String direccionEntrega) { this.direccionEntrega = direccionEntrega; }
    public String getRepartidorNombre() { return repartidorNombre; }
    public void setRepartidorNombre(String repartidorNombre) { this.repartidorNombre = repartidorNombre; }
    public Long getRepartidorUsuarioId() { return repartidorUsuarioId; }
    public void setRepartidorUsuarioId(Long repartidorUsuarioId) { this.repartidorUsuarioId = repartidorUsuarioId; }
    public boolean isCobrado() { return cobrado; }
    public void setCobrado(boolean cobrado) { this.cobrado = cobrado; }
    public Mesa getMesa() { return mesa; }
    public void setMesa(Mesa mesa) { this.mesa = mesa; }
    public Long getVentaOrigenId() { return ventaOrigenId; }
    public void setVentaOrigenId(Long ventaOrigenId) { this.ventaOrigenId = ventaOrigenId; }
    public Double getTotalNeto() { return totalNeto; }
    public void setTotalNeto(Double totalNeto) { this.totalNeto = totalNeto; }
    public Double getTotalIva() { return totalIva; }
    public void setTotalIva(Double totalIva) { this.totalIva = totalIva; }
    public Double getTotalFinal() { return totalFinal; }
    public void setTotalFinal(Double totalFinal) { this.totalFinal = totalFinal; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public Double getMontoAbonado() { return montoAbonado; }
    public void setMontoAbonado(Double montoAbonado) { this.montoAbonado = montoAbonado; }
    public Double getVuelto() { return vuelto; }
    public void setVuelto(Double vuelto) { this.vuelto = vuelto; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public List<ItemVenta> getItems() { return items; }
    public void setItems(List<ItemVenta> items) { this.items = items; }
}
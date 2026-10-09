package com.tulumcore.api.entities;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "turnos")
public class Turno extends BaseEntity {

    public static final String PEDIDO = "PEDIDO";
    public static final String CONFIRMADO = "CONFIRMADO";
    public static final String EN_ESPERA = "EN_ESPERA";
    public static final String ATENDIDO = "ATENDIDO";
    public static final String AUSENTE = "AUSENTE";
    public static final String CANCELADO = "CANCELADO";
    public static final String PARTICULAR = "PARTICULAR";
    public static final String OBRA_SOCIAL = "OBRA_SOCIAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(name = "venta_id")
    private Long ventaId;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant fin;

    @Column(name = "duracion_minutos", nullable = false)
    private int duracionMinutos = 20;

    @Column(nullable = false, length = 24)
    private String estado = PEDIDO;

    @Column(nullable = false, length = 24)
    private String modalidad = PARTICULAR;

    @Column(columnDefinition = "text")
    private String notas;

    @Column(name = "recordatorio_24h", nullable = false)
    private boolean recordatorio24h = false;

    @Column(name = "recordatorio_2h", nullable = false)
    private boolean recordatorio2h = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public Long getVentaId() { return ventaId; }
    public void setVentaId(Long ventaId) { this.ventaId = ventaId; }
    public Instant getInicio() { return inicio; }
    public void setInicio(Instant inicio) { this.inicio = inicio; }
    public Instant getFin() { return fin; }
    public void setFin(Instant fin) { this.fin = fin; }
    public int getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(int duracionMinutos) { this.duracionMinutos = duracionMinutos; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getModalidad() { return modalidad; }
    public void setModalidad(String modalidad) { this.modalidad = modalidad; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public boolean isRecordatorio24h() { return recordatorio24h; }
    public void setRecordatorio24h(boolean recordatorio24h) { this.recordatorio24h = recordatorio24h; }
    public boolean isRecordatorio2h() { return recordatorio2h; }
    public void setRecordatorio2h(boolean recordatorio2h) { this.recordatorio2h = recordatorio2h; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public boolean ocupaAgenda() {
        return !CANCELADO.equals(estado) && !AUSENTE.equals(estado);
    }
}

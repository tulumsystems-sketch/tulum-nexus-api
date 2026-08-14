package com.tulumcore.api.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditory_logs")
public class AuditoryLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accion;

    @Column(nullable = false)
    private String entidad;

    @Column(nullable = false)
    private Long entidadId;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "detalle_anterior", columnDefinition = "TEXT")
    private String detalleAnterior;

    @Column(name = "detalle_nuevo", columnDefinition = "TEXT")
    private String detalleNuevo;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getEntidad() {
        return entidad;
    }

    public void setEntidad(String entidad) {
        this.entidad = entidad;
    }

    public Long getEntidadId() {
        return entidadId;
    }

    public void setEntidadId(Long entidadId) {
        this.entidadId = entidadId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDetalleAnterior() {
        return detalleAnterior;
    }

    public void setDetalleAnterior(String detalleAnterior) {
        this.detalleAnterior = detalleAnterior;
    }

    public String getDetalleNuevo() {
        return detalleNuevo;
    }

    public void setDetalleNuevo(String detalleNuevo) {
        this.detalleNuevo = detalleNuevo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}

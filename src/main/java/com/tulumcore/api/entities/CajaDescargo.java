package com.tulumcore.api.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "caja_descargos")
public class CajaDescargo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "caja_id")
    private Caja caja;

    private LocalDateTime fecha = LocalDateTime.now();
    private Double montoAnterior;
    private Double montoNuevo;
    private Double diferencia;

    @Column(nullable = false, length = 500)
    private String motivo;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Caja getCaja() { return caja; }
    public void setCaja(Caja caja) { this.caja = caja; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Double getMontoAnterior() { return montoAnterior; }
    public void setMontoAnterior(Double montoAnterior) { this.montoAnterior = montoAnterior; }
    public Double getMontoNuevo() { return montoNuevo; }
    public void setMontoNuevo(Double montoNuevo) { this.montoNuevo = montoNuevo; }
    public Double getDiferencia() { return diferencia; }
    public void setDiferencia(Double diferencia) { this.diferencia = diferencia; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}

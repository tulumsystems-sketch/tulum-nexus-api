package com.tulumcore.api.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cajas")
public class Caja extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;

    private Double montoInicial;
    private Double montoVentasEfectivo;
    private Double montoVentasMP;
    private Double montoFinalEsperado;
    private Double montoFinalReal;

    private String estado; // "ABIERTA" o "CERRADA"

    @ManyToOne
    @JoinColumn(name = "usuario_apertura_id")
    private Usuario usuarioApertura;

    // --- Getters y Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getFechaApertura() { return fechaApertura; }
    public void setFechaApertura(LocalDateTime fechaApertura) { this.fechaApertura = fechaApertura; }

    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDateTime fechaCierre) { this.fechaCierre = fechaCierre; }

    public Double getMontoInicial() { return montoInicial; }
    public void setMontoInicial(Double montoInicial) { this.montoInicial = montoInicial; }

    public Double getMontoVentasEfectivo() { return montoVentasEfectivo; }
    public void setMontoVentasEfectivo(Double montoVentasEfectivo) { this.montoVentasEfectivo = montoVentasEfectivo; }

    public Double getMontoVentasMP() { return montoVentasMP; }
    public void setMontoVentasMP(Double montoVentasMP) { this.montoVentasMP = montoVentasMP; }

    public Double getMontoFinalEsperado() { return montoFinalEsperado; }
    public void setMontoFinalEsperado(Double montoFinalEsperado) { this.montoFinalEsperado = montoFinalEsperado; }

    public Double getMontoFinalReal() { return montoFinalReal; }
    public void setMontoFinalReal(Double montoFinalReal) { this.montoFinalReal = montoFinalReal; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Usuario getUsuarioApertura() { return usuarioApertura; }
    public void setUsuarioApertura(Usuario usuarioApertura) { this.usuarioApertura = usuarioApertura; }
}
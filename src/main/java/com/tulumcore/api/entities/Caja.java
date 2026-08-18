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
    /** Ventas cobradas por transferencia o alias: no entran al efectivo del cajón. */
    private Double montoVentasTransferencia = 0.0;
    /**
     * Cobranzas de remitos recibidas en efectivo. Se separan de las ventas del dia
     * para no ensuciar las metricas diarias, pero si entran al efectivo del cajon.
     */
    private Double montoCobranzasEfectivo = 0.0;

    /** Cobranzas de remitos recibidas por transferencia o alias: no entran al efectivo del cajon. */
    private Double montoCobranzasTransferencia = 0.0;

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

    public Double getMontoVentasTransferencia() { return montoVentasTransferencia; }
    public void setMontoVentasTransferencia(Double montoVentasTransferencia) { this.montoVentasTransferencia = montoVentasTransferencia; }

    public Double getMontoCobranzasEfectivo() { return montoCobranzasEfectivo; }
    public void setMontoCobranzasEfectivo(Double montoCobranzasEfectivo) { this.montoCobranzasEfectivo = montoCobranzasEfectivo; }

    public Double getMontoCobranzasTransferencia() { return montoCobranzasTransferencia; }
    public void setMontoCobranzasTransferencia(Double montoCobranzasTransferencia) { this.montoCobranzasTransferencia = montoCobranzasTransferencia; }

    public Double getMontoFinalEsperado() { return montoFinalEsperado; }
    public void setMontoFinalEsperado(Double montoFinalEsperado) { this.montoFinalEsperado = montoFinalEsperado; }

    public Double getMontoFinalReal() { return montoFinalReal; }
    public void setMontoFinalReal(Double montoFinalReal) { this.montoFinalReal = montoFinalReal; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Usuario getUsuarioApertura() { return usuarioApertura; }
    public void setUsuarioApertura(Usuario usuarioApertura) { this.usuarioApertura = usuarioApertura; }
}
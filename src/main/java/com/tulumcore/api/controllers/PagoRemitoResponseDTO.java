package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.PagoRemito;

import java.time.LocalDateTime;

// DTO de salida de una cobranza, sin arrastrar el remito completo
public class PagoRemitoResponseDTO {

    private Long id;
    private Long remitoId;
    private String nroRemito;
    private LocalDateTime fecha;
    private Double monto;
    private String metodoPago;
    private String observaciones;
    private String usuarioEmail;

    public static PagoRemitoResponseDTO desde(PagoRemito pago) {
        PagoRemitoResponseDTO dto = new PagoRemitoResponseDTO();
        dto.id = pago.getId();
        dto.fecha = pago.getFecha();
        dto.monto = pago.getMonto();
        dto.metodoPago = pago.getMetodoPago();
        dto.observaciones = pago.getObservaciones();
        if (pago.getRemito() != null) {
            dto.remitoId = pago.getRemito().getId();
            dto.nroRemito = pago.getRemito().getNroRemito();
        }
        if (pago.getUsuario() != null) {
            dto.usuarioEmail = pago.getUsuario().getEmail();
        }
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRemitoId() { return remitoId; }
    public void setRemitoId(Long remitoId) { this.remitoId = remitoId; }
    public String getNroRemito() { return nroRemito; }
    public void setNroRemito(String nroRemito) { this.nroRemito = nroRemito; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public String getUsuarioEmail() { return usuarioEmail; }
    public void setUsuarioEmail(String usuarioEmail) { this.usuarioEmail = usuarioEmail; }
}

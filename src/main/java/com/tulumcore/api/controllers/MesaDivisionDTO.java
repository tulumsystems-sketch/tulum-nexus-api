package com.tulumcore.api.controllers;

import java.util.List;

/** Cobrar una parte de la mesa: platos puntuales, N partes iguales, o un monto. */
public class MesaDivisionDTO {
    private List<ItemVentaDTO> items;
    private Integer partes;
    private Double monto;
    private String metodoPago;
    private Double montoAbonado;

    public List<ItemVentaDTO> getItems() { return items; }
    public void setItems(List<ItemVentaDTO> items) { this.items = items; }
    public Integer getPartes() { return partes; }
    public void setPartes(Integer partes) { this.partes = partes; }
    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public Double getMontoAbonado() { return montoAbonado; }
    public void setMontoAbonado(Double montoAbonado) { this.montoAbonado = montoAbonado; }
}

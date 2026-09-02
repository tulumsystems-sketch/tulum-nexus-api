package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.Mesa;
import com.tulumcore.api.entities.Venta;

/** Mesa + cuenta abierta hidratada (ítems, totales, cobro). */
public class MesaCuentaDTO {
    private MesaListadoDTO mesa;
    private VentaListadoDTO cuenta;
    private VentaListadoDTO parteCobrada;

    public static MesaCuentaDTO de(Mesa mesa, Venta ventaAbierta, VentaListadoDTO cuenta) {
        MesaCuentaDTO dto = new MesaCuentaDTO();
        dto.setMesa(MesaListadoDTO.desde(mesa, ventaAbierta));
        dto.setCuenta(cuenta);
        return dto;
    }

    public MesaListadoDTO getMesa() { return mesa; }
    public void setMesa(MesaListadoDTO mesa) { this.mesa = mesa; }
    public VentaListadoDTO getCuenta() { return cuenta; }
    public void setCuenta(VentaListadoDTO cuenta) { this.cuenta = cuenta; }
    public VentaListadoDTO getParteCobrada() { return parteCobrada; }
    public void setParteCobrada(VentaListadoDTO parteCobrada) { this.parteCobrada = parteCobrada; }
}

package com.tulumcore.api.controllers;

import java.util.ArrayList;
import java.util.List;

/** Staff con WhatsApp: Socio (ADMIN), Caja (OPERADOR), Delivery (REPARTIDOR). */
public class BotEquipoDTO {
    private List<BotCadeteDTO> socios = new ArrayList<>();
    private List<BotCadeteDTO> cajas = new ArrayList<>();
    private List<BotCadeteDTO> delivery = new ArrayList<>();

    public List<BotCadeteDTO> getSocios() { return socios; }
    public void setSocios(List<BotCadeteDTO> socios) { this.socios = socios != null ? socios : new ArrayList<>(); }
    public List<BotCadeteDTO> getCajas() { return cajas; }
    public void setCajas(List<BotCadeteDTO> cajas) { this.cajas = cajas != null ? cajas : new ArrayList<>(); }
    public List<BotCadeteDTO> getDelivery() { return delivery; }
    public void setDelivery(List<BotCadeteDTO> delivery) { this.delivery = delivery != null ? delivery : new ArrayList<>(); }
}

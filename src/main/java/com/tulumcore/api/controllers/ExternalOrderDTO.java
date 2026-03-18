package com.tulumcore.api.controllers;

import java.util.List;

public class ExternalOrderDTO {
    private String clienteTelefono; // El número de WhatsApp
    private List<ItemBotDTO> items;

    public String getClienteTelefono() { return clienteTelefono; }
    public void setClienteTelefono(String t) { this.clienteTelefono = t; }
    public List<ItemBotDTO> getItems() { return items; }
    public void setItems(List<ItemBotDTO> i) { this.items = i; }

    public static class ItemBotDTO {
        private Long productoId;
        private Integer cantidad;

        public Long getProductoId() { return productoId; }
        public void setProductoId(Long id) { this.productoId = id; }
        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer c) { this.cantidad = c; }
    }
}
package com.tulumcore.api.controllers;

import java.util.ArrayList;
import java.util.List;

public class SalidaPedidosDTO {
    private List<VentaListadoDTO> listos = new ArrayList<>();
    private List<VentaListadoDTO> enCamino = new ArrayList<>();

    public SalidaPedidosDTO() {}

    public SalidaPedidosDTO(List<VentaListadoDTO> listos, List<VentaListadoDTO> enCamino) {
        this.listos = listos != null ? listos : new ArrayList<>();
        this.enCamino = enCamino != null ? enCamino : new ArrayList<>();
    }

    public List<VentaListadoDTO> getListos() { return listos; }
    public void setListos(List<VentaListadoDTO> listos) { this.listos = listos; }
    public List<VentaListadoDTO> getEnCamino() { return enCamino; }
    public void setEnCamino(List<VentaListadoDTO> enCamino) { this.enCamino = enCamino; }
}

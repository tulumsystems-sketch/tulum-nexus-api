package com.tulumcore.api.controllers;

import java.util.ArrayList;
import java.util.List;

public class BotEnviosDTO {
    private List<VentaListadoDTO> enCocina = new ArrayList<>();
    private List<VentaListadoDTO> listos = new ArrayList<>();
    private List<VentaListadoDTO> enCamino = new ArrayList<>();
    private List<VentaListadoDTO> paraClientes = new ArrayList<>();

    public List<VentaListadoDTO> getEnCocina() { return enCocina; }
    public void setEnCocina(List<VentaListadoDTO> enCocina) { this.enCocina = enCocina; }
    public List<VentaListadoDTO> getListos() { return listos; }
    public void setListos(List<VentaListadoDTO> listos) { this.listos = listos; }
    public List<VentaListadoDTO> getEnCamino() { return enCamino; }
    public void setEnCamino(List<VentaListadoDTO> enCamino) { this.enCamino = enCamino; }
    public List<VentaListadoDTO> getParaClientes() { return paraClientes; }
    public void setParaClientes(List<VentaListadoDTO> paraClientes) { this.paraClientes = paraClientes; }
}

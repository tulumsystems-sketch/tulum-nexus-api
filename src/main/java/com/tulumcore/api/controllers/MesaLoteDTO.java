package com.tulumcore.api.controllers;

/** Crear varias mesas numeradas de una vez (demo Fogón: 1..12). */
public class MesaLoteDTO {
    private Integer desde;
    private Integer hasta;

    public Integer getDesde() { return desde; }
    public void setDesde(Integer desde) { this.desde = desde; }
    public Integer getHasta() { return hasta; }
    public void setHasta(Integer hasta) { this.hasta = hasta; }
}

package com.tulumcore.api.controllers;

import java.util.ArrayList;
import java.util.List;

/** Body para crear o editar una mesa. */
public class MesaDTO {
    private Integer numero;
    private String nombre;
    private Integer capacidad;
    private Boolean activa;

    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }
    public Boolean getActiva() { return activa; }
    public void setActiva(Boolean activa) { this.activa = activa; }
}

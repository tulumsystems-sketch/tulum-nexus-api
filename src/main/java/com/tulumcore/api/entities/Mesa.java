package com.tulumcore.api.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "mesas")
public class Mesa extends BaseEntity {

    public static final String LIBRE = "LIBRE";
    public static final String OCUPADA = "OCUPADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer numero;

    private String nombre;
    private Integer capacidad;

    @Column(nullable = false)
    private boolean activa = true;

    /** LIBRE u OCUPADA. Se sincroniza al abrir / liberar cuenta. */
    @Column(nullable = false)
    private String estado = LIBRE;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String etiqueta() {
        if (nombre != null && !nombre.isBlank()) {
            return nombre.trim();
        }
        return "Mesa " + numero;
    }
}

package com.tulumcore.api.controllers;

public class BotCadeteDTO {
    private Long id;
    private String nombre;
    private String telefono;
    private String email;
    /** SOCIO, CAJA o DELIVERY. Vacío en respuestas viejas. */
    private String rol;

    public BotCadeteDTO() {}

    public BotCadeteDTO(Long id, String nombre, String telefono, String email) {
        this(id, nombre, telefono, email, null);
    }

    public BotCadeteDTO(Long id, String nombre, String telefono, String email, String rol) {
        this.id = id;
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
        this.rol = rol;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}

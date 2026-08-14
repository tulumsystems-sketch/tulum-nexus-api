package com.tulumcore.api.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "proveedores")
public class Proveedor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String contacto;
    private String telefono;
    private String email;
    private String direccion;
    private String cuit;
    private String observaciones;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getContacto() { return contacto; }
    public void setContacto(String contacto) { this.contacto = contacto; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getCuit() { return cuit; }
    public void setCuit(String cuit) { this.cuit = cuit; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}

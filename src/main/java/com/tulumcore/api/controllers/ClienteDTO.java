package com.tulumcore.api.controllers;

/**
 * DTO para la transferencia de datos de Clientes.
 * Evita la recursión infinita con Cotizaciones y protege la entidad base.
 */
public class ClienteDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String empresa;
    private String telefono;
    private String direccion;
    private String googleMapsUrl;
    private Double saldoCuentaCorriente;
    private String dni;
    private String obraSocial;
    private String nroAfiliado;
    private String notaSecretaria;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmpresa() { return empresa; }
    public void setEmpresa(String empresa) { this.empresa = empresa; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getGoogleMapsUrl() { return googleMapsUrl; }
    public void setGoogleMapsUrl(String googleMapsUrl) { this.googleMapsUrl = googleMapsUrl; }

    public Double getSaldoCuentaCorriente() { return saldoCuentaCorriente; }
    public void setSaldoCuentaCorriente(Double saldoCuentaCorriente) { this.saldoCuentaCorriente = saldoCuentaCorriente; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getObraSocial() { return obraSocial; }
    public void setObraSocial(String obraSocial) { this.obraSocial = obraSocial; }
    public String getNroAfiliado() { return nroAfiliado; }
    public void setNroAfiliado(String nroAfiliado) { this.nroAfiliado = nroAfiliado; }
    public String getNotaSecretaria() { return notaSecretaria; }
    public void setNotaSecretaria(String notaSecretaria) { this.notaSecretaria = notaSecretaria; }
}
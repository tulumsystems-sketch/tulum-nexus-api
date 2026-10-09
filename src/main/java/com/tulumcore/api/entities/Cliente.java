package com.tulumcore.api.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "clientes")
public class Cliente extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String apellido;
    private String empresa;
    private String telefono;
    private String direccion;

    @Column(name = "google_maps_url")
    private String googleMapsUrl;

    @Column(name = "saldo_cuenta_corriente")
    private Double saldoCuentaCorriente = 0.0;

    /** Ficha administrativa (consultorio). No es historia clínica. */
    private String dni;

    @Column(name = "obra_social")
    private String obraSocial;

    @Column(name = "nro_afiliado")
    private String nroAfiliado;

    @Column(name = "nota_secretaria", columnDefinition = "text")
    private String notaSecretaria;

    // EL JSON IGNORE QUE SALVA EL ERROR 500
    @JsonIgnore
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
    private List<Venta> ventas;

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

    public List<Venta> getVentas() { return ventas; }
    public void setVentas(List<Venta> ventas) { this.ventas = ventas; }
}
package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.Venta;

public class PedidoCreadoDTO {
    private Long id;
    private String nro;
    private Double total;
    private String canal;
    private String estado;
    private String telefono;
    private String nombre;
    private String direccion;

    public static PedidoCreadoDTO desde(Venta venta) {
        PedidoCreadoDTO dto = new PedidoCreadoDTO();
        dto.setId(venta.getId());
        dto.setNro(venta.getNroComprobante() != null ? venta.getNroComprobante() : String.valueOf(venta.getId()));
        dto.setTotal(venta.getTotalFinal());
        dto.setCanal(venta.getCanal());
        dto.setEstado(venta.getEstado());
        dto.setTelefono(venta.getTelefonoContacto());
        dto.setNombre(venta.getNombreContacto());
        dto.setDireccion(venta.getDireccionEntrega());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNro() { return nro; }
    public void setNro(String nro) { this.nro = nro; }
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }
    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
}

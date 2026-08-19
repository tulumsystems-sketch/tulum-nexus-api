package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.Cliente;
import com.tulumcore.api.entities.Venta;

import java.time.LocalDateTime;

public class VentaListadoDTO {
    private Long id;
    private LocalDateTime fecha;
    private String nroComprobante;
    private String estado;
    private String metodoPago;
    private Double totalFinal;
    private String moneda;
    private String observaciones;
    private ClienteResumen cliente;

    public static VentaListadoDTO desde(Venta venta) {
        VentaListadoDTO dto = new VentaListadoDTO();
        dto.setId(venta.getId());
        dto.setFecha(venta.getFecha());
        dto.setNroComprobante(venta.getNroComprobante());
        dto.setEstado(venta.getEstado());
        dto.setMetodoPago(venta.getMetodoPago());
        dto.setTotalFinal(venta.getTotalFinal());
        dto.setMoneda(venta.getMoneda());
        dto.setObservaciones(venta.getObservaciones());
        Cliente cliente = venta.getCliente();
        if (cliente != null) {
            dto.setCliente(new ClienteResumen(cliente.getId(), cliente.getNombre(), cliente.getApellido()));
        }
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getNroComprobante() { return nroComprobante; }
    public void setNroComprobante(String nroComprobante) { this.nroComprobante = nroComprobante; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public Double getTotalFinal() { return totalFinal; }
    public void setTotalFinal(Double totalFinal) { this.totalFinal = totalFinal; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public ClienteResumen getCliente() { return cliente; }
    public void setCliente(ClienteResumen cliente) { this.cliente = cliente; }

    public static class ClienteResumen {
        private Long id;
        private String nombre;
        private String apellido;

        public ClienteResumen() {}

        public ClienteResumen(Long id, String nombre, String apellido) {
            this.id = id;
            this.nombre = nombre;
            this.apellido = apellido;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getApellido() { return apellido; }
        public void setApellido(String apellido) { this.apellido = apellido; }
    }
}

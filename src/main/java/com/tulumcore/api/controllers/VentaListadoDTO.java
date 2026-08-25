package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.Cliente;
import com.tulumcore.api.entities.ItemVenta;
import com.tulumcore.api.entities.Venta;
import com.tulumcore.api.services.EstadoPedido;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VentaListadoDTO {
    private Long id;
    private LocalDateTime fecha;
    private String nroComprobante;
    private String estado;
    private String canal;
    private String metodoPago;
    private Double totalFinal;
    private String moneda;
    private String observaciones;
    private String nombreContacto;
    private String telefonoContacto;
    private String direccionEntrega;
    private List<String> proximosEstados = new ArrayList<>();
    private List<ItemResumen> items = new ArrayList<>();
    private ClienteResumen cliente;

    public static VentaListadoDTO desde(Venta venta) {
        VentaListadoDTO dto = new VentaListadoDTO();
        dto.setId(venta.getId());
        dto.setFecha(venta.getFecha());
        dto.setNroComprobante(venta.getNroComprobante());
        dto.setEstado(venta.getEstado());
        dto.setCanal(venta.getCanal());
        dto.setMetodoPago(venta.getMetodoPago());
        dto.setTotalFinal(venta.getTotalFinal());
        dto.setMoneda(venta.getMoneda());
        dto.setObservaciones(venta.getObservaciones());
        dto.setNombreContacto(venta.getNombreContacto());
        dto.setTelefonoContacto(venta.getTelefonoContacto());
        dto.setDireccionEntrega(venta.getDireccionEntrega());
        dto.setProximosEstados(EstadoPedido.siguientes(venta.getEstado(), venta.getCanal()));
        Cliente cliente = venta.getCliente();
        if (cliente != null) {
            dto.setCliente(new ClienteResumen(cliente.getId(), cliente.getNombre(), cliente.getApellido(), cliente.getTelefono()));
            if (dto.getTelefonoContacto() == null || dto.getTelefonoContacto().isBlank()) {
                dto.setTelefonoContacto(cliente.getTelefono());
            }
            if (dto.getNombreContacto() == null || dto.getNombreContacto().isBlank()) {
                dto.setNombreContacto((cliente.getNombre() + " " + cliente.getApellido()).trim());
            }
            if (dto.getDireccionEntrega() == null || dto.getDireccionEntrega().isBlank()) {
                dto.setDireccionEntrega(cliente.getDireccion());
            }
        }
        if (venta.getItems() != null) {
            for (ItemVenta item : venta.getItems()) {
                String producto = item.getProducto() != null ? item.getProducto().getNombre() : "Producto";
                dto.getItems().add(new ItemResumen(producto, item.getCantidad(), item.getPrecioUnitario()));
            }
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
    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public Double getTotalFinal() { return totalFinal; }
    public void setTotalFinal(Double totalFinal) { this.totalFinal = totalFinal; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public String getNombreContacto() { return nombreContacto; }
    public void setNombreContacto(String nombreContacto) { this.nombreContacto = nombreContacto; }
    public String getTelefonoContacto() { return telefonoContacto; }
    public void setTelefonoContacto(String telefonoContacto) { this.telefonoContacto = telefonoContacto; }
    public String getDireccionEntrega() { return direccionEntrega; }
    public void setDireccionEntrega(String direccionEntrega) { this.direccionEntrega = direccionEntrega; }
    public List<String> getProximosEstados() { return proximosEstados; }
    public void setProximosEstados(List<String> proximosEstados) { this.proximosEstados = proximosEstados; }
    public List<ItemResumen> getItems() { return items; }
    public void setItems(List<ItemResumen> items) { this.items = items; }
    public ClienteResumen getCliente() { return cliente; }
    public void setCliente(ClienteResumen cliente) { this.cliente = cliente; }

    public static class ItemResumen {
        private String producto;
        private Integer cantidad;
        private Double precioUnitario;

        public ItemResumen() {}

        public ItemResumen(String producto, Integer cantidad, Double precioUnitario) {
            this.producto = producto;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
        }

        public String getProducto() { return producto; }
        public void setProducto(String producto) { this.producto = producto; }
        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
        public Double getPrecioUnitario() { return precioUnitario; }
        public void setPrecioUnitario(Double precioUnitario) { this.precioUnitario = precioUnitario; }
    }

    public static class ClienteResumen {
        private Long id;
        private String nombre;
        private String apellido;
        private String telefono;

        public ClienteResumen() {}

        public ClienteResumen(Long id, String nombre, String apellido, String telefono) {
            this.id = id;
            this.nombre = nombre;
            this.apellido = apellido;
            this.telefono = telefono;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getApellido() { return apellido; }
        public void setApellido(String apellido) { this.apellido = apellido; }
        public String getTelefono() { return telefono; }
        public void setTelefono(String telefono) { this.telefono = telefono; }
    }
}

package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.Cliente;
import com.tulumcore.api.entities.ItemVenta;
import com.tulumcore.api.entities.Venta;
import com.tulumcore.api.services.EstadoPedido;
import org.hibernate.Hibernate;

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
    private String repartidorNombre;
    private Long repartidorUsuarioId;
    private boolean cobrado;
    private boolean puedeTomar;
    private boolean puedeLiberar;
    private Long mesaId;
    private Integer mesaNumero;
    private String mesaEtiqueta;
    private Long ventaOrigenId;
    private Double montoPagado;
    private Double saldo;
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
        dto.setRepartidorNombre(venta.getRepartidorNombre());
        dto.setRepartidorUsuarioId(venta.getRepartidorUsuarioId());
        dto.setCobrado(venta.isCobrado());
        if (venta.getMesa() != null) {
            dto.setMesaId(venta.getMesa().getId());
            dto.setMesaNumero(venta.getMesa().getNumero());
            dto.setMesaEtiqueta(venta.getMesa().etiqueta());
        }
        dto.setVentaOrigenId(venta.getVentaOrigenId());
        dto.setProximosEstados(EstadoPedido.siguientes(venta.getEstado(), venta.getCanal(), venta.getDireccionEntrega()));
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
        if (venta.getItems() != null && Hibernate.isInitialized(venta.getItems())) {
            for (ItemVenta item : venta.getItems()) {
                String producto = item.getProducto() != null ? item.getProducto().getNombre() : "Producto";
                Long productoId = item.getProducto() != null ? item.getProducto().getId() : null;
                dto.getItems().add(new ItemResumen(
                        item.getId(),
                        productoId,
                        producto,
                        item.getCantidad(),
                        item.getPrecioUnitario(),
                        item.getObservaciones()));
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
    public String getRepartidorNombre() { return repartidorNombre; }
    public void setRepartidorNombre(String repartidorNombre) { this.repartidorNombre = repartidorNombre; }
    public Long getRepartidorUsuarioId() { return repartidorUsuarioId; }
    public void setRepartidorUsuarioId(Long repartidorUsuarioId) { this.repartidorUsuarioId = repartidorUsuarioId; }
    public boolean isCobrado() { return cobrado; }
    public void setCobrado(boolean cobrado) { this.cobrado = cobrado; }
    public boolean isPuedeTomar() { return puedeTomar; }
    public void setPuedeTomar(boolean puedeTomar) { this.puedeTomar = puedeTomar; }
    public boolean isPuedeLiberar() { return puedeLiberar; }
    public void setPuedeLiberar(boolean puedeLiberar) { this.puedeLiberar = puedeLiberar; }
    public Long getMesaId() { return mesaId; }
    public void setMesaId(Long mesaId) { this.mesaId = mesaId; }
    public Integer getMesaNumero() { return mesaNumero; }
    public void setMesaNumero(Integer mesaNumero) { this.mesaNumero = mesaNumero; }
    public String getMesaEtiqueta() { return mesaEtiqueta; }
    public void setMesaEtiqueta(String mesaEtiqueta) { this.mesaEtiqueta = mesaEtiqueta; }
    public Long getVentaOrigenId() { return ventaOrigenId; }
    public void setVentaOrigenId(Long ventaOrigenId) { this.ventaOrigenId = ventaOrigenId; }
    public Double getMontoPagado() { return montoPagado; }
    public void setMontoPagado(Double montoPagado) { this.montoPagado = montoPagado; }
    public Double getSaldo() { return saldo; }
    public void setSaldo(Double saldo) { this.saldo = saldo; }
    public List<String> getProximosEstados() { return proximosEstados; }
    public void setProximosEstados(List<String> proximosEstados) { this.proximosEstados = proximosEstados; }
    public List<ItemResumen> getItems() { return items; }
    public void setItems(List<ItemResumen> items) { this.items = items; }
    public ClienteResumen getCliente() { return cliente; }
    public void setCliente(ClienteResumen cliente) { this.cliente = cliente; }

    public static class ItemResumen {
        private Long id;
        private Long productoId;
        private String producto;
        private Integer cantidad;
        private Double precioUnitario;
        private String observaciones;

        public ItemResumen() {}

        public ItemResumen(Long id, Long productoId, String producto, Integer cantidad,
                           Double precioUnitario, String observaciones) {
            this.id = id;
            this.productoId = productoId;
            this.producto = producto;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
            this.observaciones = observaciones;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getProductoId() { return productoId; }
        public void setProductoId(Long productoId) { this.productoId = productoId; }
        public String getProducto() { return producto; }
        public void setProducto(String producto) { this.producto = producto; }
        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
        public Double getPrecioUnitario() { return precioUnitario; }
        public void setPrecioUnitario(Double precioUnitario) { this.precioUnitario = precioUnitario; }
        public String getObservaciones() { return observaciones; }
        public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
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

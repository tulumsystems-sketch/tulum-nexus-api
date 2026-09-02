package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.ItemVenta;
import com.tulumcore.api.entities.Mesa;
import com.tulumcore.api.entities.Venta;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Mesa con resumen de la cuenta abierta, si hay. */
public class MesaListadoDTO {
    private Long id;
    private Integer numero;
    private String nombre;
    private String etiqueta;
    private Integer capacidad;
    private boolean activa;
    private String estado;
    private Long ventaId;
    private String nroComprobante;
    private Double totalFinal;
    private LocalDateTime abiertaDesde;
    private boolean cobrado;
    private String estadoCuenta;
    private List<String> platos = new ArrayList<>();

    public static MesaListadoDTO desde(Mesa mesa, Venta ventaAbierta) {
        MesaListadoDTO dto = new MesaListadoDTO();
        dto.setId(mesa.getId());
        dto.setNumero(mesa.getNumero());
        dto.setNombre(mesa.getNombre());
        dto.setEtiqueta(mesa.etiqueta());
        dto.setCapacidad(mesa.getCapacidad());
        dto.setActiva(mesa.isActiva());
        dto.setEstado(mesa.getEstado());
        if (ventaAbierta != null) {
            dto.setVentaId(ventaAbierta.getId());
            dto.setNroComprobante(ventaAbierta.getNroComprobante());
            dto.setTotalFinal(ventaAbierta.getTotalFinal());
            dto.setAbiertaDesde(ventaAbierta.getFecha());
            dto.setCobrado(ventaAbierta.isCobrado());
            dto.setEstadoCuenta(ventaAbierta.getEstado());
            dto.setPlatos(resumenPlatos(ventaAbierta));
        }
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEtiqueta() { return etiqueta; }
    public void setEtiqueta(String etiqueta) { this.etiqueta = etiqueta; }
    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Long getVentaId() { return ventaId; }
    public void setVentaId(Long ventaId) { this.ventaId = ventaId; }
    public String getNroComprobante() { return nroComprobante; }
    public void setNroComprobante(String nroComprobante) { this.nroComprobante = nroComprobante; }
    public Double getTotalFinal() { return totalFinal; }
    public void setTotalFinal(Double totalFinal) { this.totalFinal = totalFinal; }
    public LocalDateTime getAbiertaDesde() { return abiertaDesde; }
    public void setAbiertaDesde(LocalDateTime abiertaDesde) { this.abiertaDesde = abiertaDesde; }
    public boolean isCobrado() { return cobrado; }
    public void setCobrado(boolean cobrado) { this.cobrado = cobrado; }
    public String getEstadoCuenta() { return estadoCuenta; }
    public void setEstadoCuenta(String estadoCuenta) { this.estadoCuenta = estadoCuenta; }
    public List<String> getPlatos() { return platos; }
    public void setPlatos(List<String> platos) { this.platos = platos != null ? platos : new ArrayList<>(); }

    private static List<String> resumenPlatos(Venta venta) {
        List<String> platos = new ArrayList<>();
        if (venta.getItems() == null) {
            return platos;
        }
        for (ItemVenta item : venta.getItems()) {
            String nombre = item.getProducto() != null ? item.getProducto().getNombre() : "Ítem";
            int cantidad = item.getCantidad() != null ? item.getCantidad() : 0;
            platos.add(cantidad + "× " + nombre);
        }
        return platos;
    }
}

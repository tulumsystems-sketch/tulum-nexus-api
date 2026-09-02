package com.tulumcore.api.controllers;

import java.util.ArrayList;
import java.util.List;

/**
 * Foto de la operación para el bot. El campo {@code rol} es SOCIO o CAJA;
 * en CAJA van en null ventas del día, stock, delivery y envíos.
 */
public class BotOperacionDTO {
    private String rol;
    private CajaVista caja;
    private VentasHoy ventasHoy;
    private MesasVista mesas;
    private List<VentaListadoDTO> cocina = new ArrayList<>();
    private List<VentaListadoDTO> retirosListos = new ArrayList<>();
    private List<VentaListadoDTO> enviosListos;
    private List<VentaListadoDTO> enCamino;
    private List<BotCadeteDTO> delivery;
    private List<StockItem> stockBajo;
    private List<BotProductoDTO> carta;

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public CajaVista getCaja() { return caja; }
    public void setCaja(CajaVista caja) { this.caja = caja; }
    public VentasHoy getVentasHoy() { return ventasHoy; }
    public void setVentasHoy(VentasHoy ventasHoy) { this.ventasHoy = ventasHoy; }
    public MesasVista getMesas() { return mesas; }
    public void setMesas(MesasVista mesas) { this.mesas = mesas; }
    public List<VentaListadoDTO> getCocina() { return cocina; }
    public void setCocina(List<VentaListadoDTO> cocina) { this.cocina = cocina != null ? cocina : new ArrayList<>(); }
    public List<VentaListadoDTO> getRetirosListos() { return retirosListos; }
    public void setRetirosListos(List<VentaListadoDTO> retirosListos) {
        this.retirosListos = retirosListos != null ? retirosListos : new ArrayList<>();
    }
    public List<VentaListadoDTO> getEnviosListos() { return enviosListos; }
    public void setEnviosListos(List<VentaListadoDTO> enviosListos) { this.enviosListos = enviosListos; }
    public List<VentaListadoDTO> getEnCamino() { return enCamino; }
    public void setEnCamino(List<VentaListadoDTO> enCamino) { this.enCamino = enCamino; }
    public List<BotCadeteDTO> getDelivery() { return delivery; }
    public void setDelivery(List<BotCadeteDTO> delivery) { this.delivery = delivery; }
    public List<StockItem> getStockBajo() { return stockBajo; }
    public void setStockBajo(List<StockItem> stockBajo) { this.stockBajo = stockBajo; }
    public List<BotProductoDTO> getCarta() { return carta; }
    public void setCarta(List<BotProductoDTO> carta) { this.carta = carta; }

    public static class CajaVista {
        private boolean abierta;
        private Double esperado;
        private Double horasAbierta;
        private Double inicial;
        private Double efectivo;
        private Double mercadoPago;
        private Double transferencia;

        public boolean isAbierta() { return abierta; }
        public void setAbierta(boolean abierta) { this.abierta = abierta; }
        public Double getEsperado() { return esperado; }
        public void setEsperado(Double esperado) { this.esperado = esperado; }
        public Double getHorasAbierta() { return horasAbierta; }
        public void setHorasAbierta(Double horasAbierta) { this.horasAbierta = horasAbierta; }
        public Double getInicial() { return inicial; }
        public void setInicial(Double inicial) { this.inicial = inicial; }
        public Double getEfectivo() { return efectivo; }
        public void setEfectivo(Double efectivo) { this.efectivo = efectivo; }
        public Double getMercadoPago() { return mercadoPago; }
        public void setMercadoPago(Double mercadoPago) { this.mercadoPago = mercadoPago; }
        public Double getTransferencia() { return transferencia; }
        public void setTransferencia(Double transferencia) { this.transferencia = transferencia; }
    }

    public static class VentasHoy {
        private long cantidad;
        private double efectivo;
        private double mercadoPago;
        private double transferencia;
        private double total;

        public long getCantidad() { return cantidad; }
        public void setCantidad(long cantidad) { this.cantidad = cantidad; }
        public double getEfectivo() { return efectivo; }
        public void setEfectivo(double efectivo) { this.efectivo = efectivo; }
        public double getMercadoPago() { return mercadoPago; }
        public void setMercadoPago(double mercadoPago) { this.mercadoPago = mercadoPago; }
        public double getTransferencia() { return transferencia; }
        public void setTransferencia(double transferencia) { this.transferencia = transferencia; }
        public double getTotal() { return total; }
        public void setTotal(double total) { this.total = total; }
    }

    public static class MesasVista {
        private int libres;
        private int ocupadas;
        private List<MesaItem> detalle = new ArrayList<>();

        public int getLibres() { return libres; }
        public void setLibres(int libres) { this.libres = libres; }
        public int getOcupadas() { return ocupadas; }
        public void setOcupadas(int ocupadas) { this.ocupadas = ocupadas; }
        public List<MesaItem> getDetalle() { return detalle; }
        public void setDetalle(List<MesaItem> detalle) { this.detalle = detalle != null ? detalle : new ArrayList<>(); }
    }

    public static class MesaItem {
        private String etiqueta;
        private Double total;
        private List<String> platos = new ArrayList<>();

        public String getEtiqueta() { return etiqueta; }
        public void setEtiqueta(String etiqueta) { this.etiqueta = etiqueta; }
        public Double getTotal() { return total; }
        public void setTotal(Double total) { this.total = total; }
        public List<String> getPlatos() { return platos; }
        public void setPlatos(List<String> platos) { this.platos = platos != null ? platos : new ArrayList<>(); }
    }

    public static class StockItem {
        private String nombre;
        private Double cantidad;
        private Integer minimo;
        private String unidad;

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public Double getCantidad() { return cantidad; }
        public void setCantidad(Double cantidad) { this.cantidad = cantidad; }
        public Integer getMinimo() { return minimo; }
        public void setMinimo(Integer minimo) { this.minimo = minimo; }
        public String getUnidad() { return unidad; }
        public void setUnidad(String unidad) { this.unidad = unidad; }
    }
}

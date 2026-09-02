package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.controllers.BotOperacionDTO;
import com.tulumcore.api.controllers.BotProductoDTO;
import com.tulumcore.api.controllers.MesaListadoDTO;
import com.tulumcore.api.controllers.VentaResumenDTO;
import com.tulumcore.api.entities.Caja;
import com.tulumcore.api.entities.FeatureKey;
import com.tulumcore.api.entities.Mesa;
import com.tulumcore.api.entities.Producto;
import com.tulumcore.api.entities.ProductoTipo;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.repositories.ProductoRecetaRepository;
import com.tulumcore.api.repositories.TenantConfigRepository;
import com.tulumcore.api.entities.TenantConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BotOperacionService {

    @Autowired private VentaService ventaService;
    @Autowired private CajaService cajaService;
    @Autowired private MesaService mesaService;
    @Autowired private ProductoService productoService;
    @Autowired private RecetaService recetaService;
    @Autowired private ProductoRecetaRepository recetaRepository;
    @Autowired private TenantFeatureService tenantFeatureService;
    @Autowired private TenantConfigRepository tenantConfigRepository;

    public java.util.Map<String, String> cobroPublico() {
        String tenant = TenantContext.getCurrentTenant();
        TenantConfig cfg = tenantConfigRepository.findByTenantId(tenant).orElse(null);
        String alias = cfg != null && cfg.getAliasCobro() != null ? cfg.getAliasCobro().trim() : "";
        return java.util.Map.of("alias", alias, "cbu", "");
    }

    public BotOperacionDTO operacion(String telefono) {
        String rol = ventaService.rolOperacionBot(telefono);
        boolean socio = "SOCIO".equals(rol);

        BotOperacionDTO dto = new BotOperacionDTO();
        dto.setRol(rol);
        dto.setCaja(cajaVista(socio));
        dto.setMesas(mesasVista());
        dto.setCocina(ventaService.obtenerCocinaLocalParaBot());
        dto.setRetirosListos(ventaService.obtenerRetirosListosParaBot());

        if (socio) {
            dto.setVentasHoy(ventasHoy());
            var envios = ventaService.obtenerEnviosParaBot();
            dto.setEnviosListos(envios.getListos());
            dto.setEnCamino(envios.getEnCamino());
            dto.setDelivery(ventaService.listarCadetesWhatsApp());
            dto.setStockBajo(stockBajoDeposito());
            dto.setCarta(cartaLectura());
        }
        return dto;
    }

    private BotOperacionDTO.CajaVista cajaVista(boolean socio) {
        BotOperacionDTO.CajaVista vista = new BotOperacionDTO.CajaVista();
        Caja caja = cajaService.obtenerCajaAbiertaActualizada().orElse(null);
        if (caja == null || !"ABIERTA".equalsIgnoreCase(caja.getEstado())) {
            vista.setAbierta(false);
            return vista;
        }
        vista.setAbierta(true);
        vista.setEsperado(caja.getMontoFinalEsperado());
        vista.setHorasAbierta(caja.getHorasAbierta());
        if (socio) {
            vista.setInicial(caja.getMontoInicial());
            vista.setEfectivo(caja.getMontoVentasEfectivo());
            vista.setMercadoPago(caja.getMontoVentasMP());
            vista.setTransferencia(caja.getMontoVentasTransferencia());
        }
        return vista;
    }

    private BotOperacionDTO.VentasHoy ventasHoy() {
        String tenant = TenantContext.getCurrentTenant();
        VentaResumenDTO resumen = ventaService.obtenerResumenHoy(tenant);
        BotOperacionDTO.VentasHoy hoy = new BotOperacionDTO.VentasHoy();
        double efectivo = n(resumen.getEfectivo());
        double mp = n(resumen.getMercadoPago());
        double trans = n(resumen.getTransferencia());
        hoy.setEfectivo(efectivo);
        hoy.setMercadoPago(mp);
        hoy.setTransferencia(trans);
        hoy.setTotal(efectivo + mp + trans);
        hoy.setCantidad(ventaService.contarVentasNoAnuladasHoy(tenant));
        return hoy;
    }

    private BotOperacionDTO.MesasVista mesasVista() {
        BotOperacionDTO.MesasVista vista = new BotOperacionDTO.MesasVista();
        if (!tenantFeatureService.isEnabled(FeatureKey.MESAS)) {
            return vista;
        }
        List<MesaListadoDTO> mesas;
        try {
            mesas = mesaService.listar();
        } catch (BusinessException e) {
            return vista;
        }
        List<BotOperacionDTO.MesaItem> ocupadas = new ArrayList<>();
        int libres = 0;
        for (MesaListadoDTO mesa : mesas) {
            if (!mesa.isActiva()) {
                continue;
            }
            if (Mesa.OCUPADA.equalsIgnoreCase(mesa.getEstado())) {
                BotOperacionDTO.MesaItem item = new BotOperacionDTO.MesaItem();
                item.setEtiqueta(mesa.getEtiqueta() != null ? mesa.getEtiqueta() : ("Mesa " + mesa.getNumero()));
                item.setTotal(mesa.getTotalFinal());
                item.setPlatos(mesa.getPlatos());
                ocupadas.add(item);
            } else {
                libres++;
            }
        }
        vista.setLibres(libres);
        vista.setOcupadas(ocupadas.size());
        vista.setDetalle(ocupadas);
        return vista;
    }

    private List<BotOperacionDTO.StockItem> stockBajoDeposito() {
        String tenant = TenantContext.getCurrentTenant();
        List<BotOperacionDTO.StockItem> out = new ArrayList<>();
        for (Producto p : productoService.getAllProductos()) {
            if (p.getStockMinimo() == null || p.getStockMinimo() <= 0) {
                continue;
            }
            boolean tieneReceta = recetaRepository.existsByProductoIdAndTenantId(p.getId(), tenant);
            boolean deposito = ProductoTipo.esInsumo(p.getTipo()) || !tieneReceta;
            if (!deposito) {
                continue;
            }
            double qty = p.getCantidadStock() != null ? p.getCantidadStock() : 0;
            if (qty > p.getStockMinimo()) {
                continue;
            }
            BotOperacionDTO.StockItem item = new BotOperacionDTO.StockItem();
            item.setNombre(p.getNombre());
            item.setCantidad(qty);
            item.setMinimo(p.getStockMinimo());
            if (p.getCategoria() != null) {
                item.setUnidad(p.getCategoria().getUnidadMedida());
            }
            out.add(item);
        }
        return out;
    }

    private List<BotProductoDTO> cartaLectura() {
        return productoService.getAllProductos().stream()
                .filter(Producto::isVendible)
                .limit(40)
                .map(this::aDto)
                .toList();
    }

    public List<BotProductoDTO> cartaPublica() {
        return productoService.getAllProductos().stream()
                .filter(this::disponibleEnCarta)
                .limit(40)
                .map(this::aDto)
                .toList();
    }

    public List<BotProductoDTO> buscarPublico(String query) {
        return productoService.buscarPorNombre(query).stream()
                .filter(this::disponibleEnCarta)
                .map(this::aDto)
                .toList();
    }

    public boolean disponibleEnCarta(Producto p) {
        if (p == null || !p.isVendible()) {
            return false;
        }
        Double porciones = recetaService.porcionesEstimadas(p);
        return porciones != null && porciones > 0;
    }

    public BotProductoDTO aDto(Producto producto) {
        Double disponible = recetaService.porcionesEstimadas(producto);
        return new BotProductoDTO(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getCantidadStock(),
                producto.getMedidas(),
                producto.getImageUrl(),
                disponible,
                producto.getCategoria() != null ? producto.getCategoria().getNombre() : "Carta"
        );
    }

    private static double n(Double v) {
        return v != null ? v : 0;
    }
}

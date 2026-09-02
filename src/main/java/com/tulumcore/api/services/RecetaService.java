package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.controllers.RecetaLineaDTO;
import com.tulumcore.api.entities.MovementType;
import com.tulumcore.api.entities.Producto;
import com.tulumcore.api.entities.ProductoReceta;
import com.tulumcore.api.entities.ProductoTipo;
import com.tulumcore.api.entities.Usuario;
import com.tulumcore.api.entities.Venta;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.repositories.ProductoRecetaRepository;
import com.tulumcore.api.repositories.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecetaService {

    @Autowired private ProductoRecetaRepository recetaRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private StockMovementService stockMovementService;

    public List<RecetaLineaDTO> listar(Long productoId) {
        String tenant = TenantContext.getCurrentTenant();
        List<RecetaLineaDTO> lineas = new ArrayList<>();
        for (ProductoReceta linea : recetaRepository.findByProductoIdAndTenantIdOrderByIdAsc(productoId, tenant)) {
            Producto insumo = linea.getInsumo();
            lineas.add(new RecetaLineaDTO(
                    insumo.getId(),
                    insumo.getNombre(),
                    linea.getCantidad(),
                    unidad(insumo)
            ));
        }
        return lineas;
    }

    @Transactional
    public void guardar(Producto plato, List<RecetaLineaDTO> lineas) {
        String tenant = TenantContext.getCurrentTenant();
        recetaRepository.deleteByProductoIdAndTenantId(plato.getId(), tenant);
        if (lineas == null || lineas.isEmpty()) {
            return;
        }
        if (ProductoTipo.esInsumo(plato.getTipo())) {
            throw new BusinessException("El artículo de depósito no lleva receta. La receta va en el plato de la carta.");
        }
        for (RecetaLineaDTO dto : lineas) {
            if (dto == null || dto.getInsumoId() == null) {
                continue;
            }
            if (dto.getInsumoId().equals(plato.getId())) {
                throw new BusinessException("Un plato no puede ser ingrediente de sí mismo.");
            }
            double cantidad = dto.getCantidad() != null ? dto.getCantidad() : 0;
            if (cantidad <= 0) {
                throw new BusinessException("La cantidad de cada ingrediente tiene que ser mayor a cero.");
            }
            Producto insumo = productoRepository.findByIdAndTenantId(dto.getInsumoId(), tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Ingrediente no encontrado: " + dto.getInsumoId()));
            if (!ProductoTipo.esInsumo(insumo.getTipo())) {
                throw new BusinessException(insumo.getNombre() + " tiene que ser un artículo de depósito, no un plato de la carta.");
            }
            ProductoReceta linea = new ProductoReceta();
            linea.setTenantId(tenant);
            linea.setProducto(plato);
            linea.setInsumo(insumo);
            linea.setCantidad(redondear(cantidad));
            recetaRepository.save(linea);
        }
    }

    public Double porcionesEstimadas(Producto plato) {
        Map<Long, Double> receta = recetaPorInsumo(plato.getId());
        if (receta.isEmpty()) {
            return stock(plato);
        }
        Double minimo = null;
        String tenant = TenantContext.getCurrentTenant();
        for (Map.Entry<Long, Double> entry : receta.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            Producto insumo = productoRepository.findByIdAndTenantId(entry.getKey(), tenant).orElse(null);
            if (insumo == null) {
                return 0.0;
            }
            double posibles = Math.floor(stock(insumo) / entry.getValue());
            if (minimo == null || posibles < minimo) {
                minimo = posibles;
            }
        }
        return minimo == null ? 0.0 : minimo;
    }

    public void exigirDisponible(Map<Long, Producto> productos, Map<Long, Integer> porcionesPorProducto) {
        Map<Long, Double> demanda = new LinkedHashMap<>();
        Map<Long, Producto> insumos = new LinkedHashMap<>();
        String tenant = TenantContext.getCurrentTenant();
        for (Map.Entry<Long, Integer> entry : porcionesPorProducto.entrySet()) {
            Producto plato = productos.get(entry.getKey());
            if (plato == null || entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            for (Map.Entry<Long, Double> linea : explosion(plato, entry.getValue()).entrySet()) {
                demanda.merge(linea.getKey(), linea.getValue(), Double::sum);
                if (!insumos.containsKey(linea.getKey())) {
                    Producto insumo = productoRepository.findByIdAndTenantId(linea.getKey(), tenant)
                            .orElseThrow(() -> new ResourceNotFoundException("Ingrediente no encontrado: " + linea.getKey()));
                    insumos.put(linea.getKey(), insumo);
                }
            }
        }
        for (Map.Entry<Long, Double> entry : demanda.entrySet()) {
            Producto insumo = insumos.get(entry.getKey());
            double disponible = stock(insumo);
            if (disponible + 0.0001 < entry.getValue()) {
                throw new BusinessException("Stock insuficiente de " + insumo.getNombre()
                        + ". Disponible: " + trim(disponible)
                        + ", necesario: " + trim(entry.getValue()) + ".");
            }
        }
    }

    public void aplicarVenta(Producto plato, double porciones, Usuario usuario, Venta venta, boolean devolver) {
        if (porciones <= 0) {
            return;
        }
        Map<Long, Double> explosion = explosion(plato, porciones);
        String tenant = TenantContext.getCurrentTenant();
        for (Map.Entry<Long, Double> entry : explosion.entrySet()) {
            double cantidad = redondear(entry.getValue());
            if (cantidad <= 0) {
                continue;
            }
            Producto insumo = productoRepository.findByIdAndTenantId(entry.getKey(), tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Ingrediente no encontrado: " + entry.getKey()));
            String motivo = devolver
                    ? "Devolución receta " + plato.getNombre() + " x" + trim(porciones)
                    : "Venta " + plato.getNombre() + " x" + trim(porciones);
            stockMovementService.registrar(
                    devolver ? MovementType.AJUSTE : MovementType.VENTA,
                    insumo, usuario, cantidad, motivo, venta, null);
        }
    }

    private Map<Long, Double> explosion(Producto plato, double porciones) {
        Map<Long, Double> receta = recetaPorInsumo(plato.getId());
        Map<Long, Double> out = new LinkedHashMap<>();
        if (receta.isEmpty()) {
            out.put(plato.getId(), porciones);
            return out;
        }
        for (Map.Entry<Long, Double> entry : receta.entrySet()) {
            out.put(entry.getKey(), redondear(entry.getValue() * porciones));
        }
        return out;
    }

    private Map<Long, Double> recetaPorInsumo(Long productoId) {
        String tenant = TenantContext.getCurrentTenant();
        Map<Long, Double> map = new LinkedHashMap<>();
        for (ProductoReceta linea : recetaRepository.findByProductoIdAndTenantIdOrderByIdAsc(productoId, tenant)) {
            if (linea.getInsumo() != null && linea.getCantidad() != null) {
                map.merge(linea.getInsumo().getId(), linea.getCantidad(), Double::sum);
            }
        }
        return map;
    }

    private double stock(Producto producto) {
        return producto.getCantidadStock() != null ? producto.getCantidadStock() : 0;
    }

    private String unidad(Producto producto) {
        if (producto.getCategoria() != null && producto.getCategoria().getUnidadMedida() != null
                && !producto.getCategoria().getUnidadMedida().isBlank()) {
            return producto.getCategoria().getUnidadMedida();
        }
        return "UNIDAD";
    }

    private double redondear(double valor) {
        return Math.round(valor * 1000.0) / 1000.0;
    }

    private String trim(double valor) {
        if (Math.abs(valor - Math.round(valor)) < 0.0001) {
            return String.valueOf(Math.round(valor));
        }
        return String.valueOf(redondear(valor));
    }
}

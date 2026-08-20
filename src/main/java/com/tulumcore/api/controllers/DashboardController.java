package com.tulumcore.api.controllers;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.Producto;
import com.tulumcore.api.entities.StockMovement;
import com.tulumcore.api.repositories.ProductoRepository;
import com.tulumcore.api.repositories.StockMovementRepository;
import com.tulumcore.api.repositories.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @GetMapping
    public Map<String, Object> resumen() {
        String tenant = TenantContext.getCurrentTenant();

        List<Producto> productos = productoRepository.findAllByTenantId(tenant);
        int totalProductos = productos.size();
        double stockTotal = productos.stream()
                .mapToDouble(p -> p.getCantidadStock() != null ? p.getCantidadStock() : 0)
                .sum();
        long bajoStock = productos.stream()
                .filter(p -> p.getStockMinimo() != null && p.getStockMinimo() > 0
                        && (p.getCantidadStock() != null ? p.getCantidadStock() : 0) <= p.getStockMinimo())
                .count();

        LocalDateTime inicioMes = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        double ventasDelMes = ventaRepository.findByTenantIdAndFechaAfter(tenant, inicioMes)
                .stream()
                .filter(v -> !"ANULADA".equals(v.getEstado()))
                .mapToDouble(v -> v.getTotalFinal() != null ? v.getTotalFinal() : 0)
                .sum();

        List<StockMovement> ultimosMovimientos = stockMovementRepository
                .findFirst10ByTenantIdOrderByFechaDesc(tenant);

        List<Producto> criticos = productos.stream()
                .filter(p -> p.getStockMinimo() != null && p.getStockMinimo() > 0
                        && (p.getCantidadStock() != null ? p.getCantidadStock() : 0) <= p.getStockMinimo())
                .toList();

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("totalProductos", totalProductos);
        resumen.put("stockTotal", stockTotal);
        resumen.put("productosBajoStock", bajoStock);
        resumen.put("ventasDelMes", ventasDelMes);
        resumen.put("ultimosMovimientos", ultimosMovimientos);
        resumen.put("productosCriticos", criticos);

        return resumen;
    }
}

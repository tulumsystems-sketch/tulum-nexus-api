package com.tulumcore.api.controllers;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.*;
import com.tulumcore.api.repositories.ProductoRepository;
import com.tulumcore.api.repositories.UsuarioRepository;
import com.tulumcore.api.services.StockMovementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/stock-movements")
public class StockMovementController {

    @Autowired
    private StockMovementService service;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false) String tipoMovimiento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {

        if (tipoMovimiento != null || desde != null || hasta != null) {
            MovementType tipo = tipoMovimiento != null ? MovementType.valueOf(tipoMovimiento) : null;
            return ResponseEntity.ok(service.buscarPorFiltros(tipo, desde, hasta));
        }
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/producto/{productoId}")
    public ResponseEntity<?> listarPorProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(service.listarPorProducto(productoId));
    }

    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody Map<String, Object> body) {
        try {
            Long productoId = Long.valueOf(body.get("productoId").toString());
            Long usuarioId = Long.valueOf(body.get("usuarioId").toString());
            String tipo = (String) body.get("tipoMovimiento");
            double cantidad = Double.parseDouble(body.get("cantidad").toString().replace(',', '.'));
            String motivo = (String) body.get("motivo");

            String tenant = TenantContext.getCurrentTenant();

            Producto producto = productoRepository.findByIdAndTenantId(productoId, tenant)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
            Usuario usuario = usuarioRepository.findByIdAndTenantId(usuarioId, tenant)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            MovementType tipoMov = MovementType.valueOf(tipo);
            StockMovement mov = service.registrar(tipoMov, producto, usuario, cantidad, motivo, null, null);

            return ResponseEntity.ok(mov);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

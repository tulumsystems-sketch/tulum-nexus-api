package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.Venta;
import com.tulumcore.api.repositories.VentaRepository;
import com.tulumcore.api.services.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private VentaRepository ventaRepository;

    @PostMapping("/link/{id}")
    public Map<String, String> obtenerLink(@PathVariable Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        String url = paymentService.crearLinkDePago(venta);
        return Map.of("url", url);
    }
}
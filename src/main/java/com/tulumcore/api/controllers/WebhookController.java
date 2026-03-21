package com.tulumcore.api.controllers;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.TenantConfig;
import com.tulumcore.api.entities.Venta;
import com.tulumcore.api.repositories.TenantConfigRepository;
import com.tulumcore.api.repositories.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    @Autowired private VentaRepository ventaRepository;
    @Autowired private TenantConfigRepository tenantConfigRepository;

    /**
     * MercadoPago envía notificaciones a esta URL cuando un pago cambia de estado.
     * URL configurada en PaymentService: /api/webhook/pagos?tenant={tenantId}
     */
    @PostMapping("/pagos")
    public ResponseEntity<Void> recibirNotificacion(
            @RequestParam String tenant,
            @RequestBody Map<String, Object> body) {

        try {
            String tipo = (String) body.get("type");
            if (!"payment".equals(tipo)) {
                return ResponseEntity.ok().build();
            }

            // Obtenemos el ID del pago desde la notificación
            Object dataObj = body.get("data");
            if (!(dataObj instanceof Map)) return ResponseEntity.ok().build();

            String paymentId = String.valueOf(((Map<?, ?>) dataObj).get("id"));

            // Configuramos el token del tenant
            TenantConfig config = tenantConfigRepository.findByTenantId(tenant)
                    .orElse(null);
            if (config == null || config.getMpAccessToken() == null) {
                return ResponseEntity.ok().build();
            }

            MercadoPagoConfig.setAccessToken(config.getMpAccessToken());

            // Consultamos el pago real en MercadoPago
            PaymentClient paymentClient = new PaymentClient();
            Payment payment = paymentClient.get(Long.parseLong(paymentId));

            String externalRef = payment.getExternalReference();
            String status = payment.getStatus();

            if (externalRef == null) return ResponseEntity.ok().build();

            // Buscamos la venta por external_reference (es el ID de venta)
            TenantContext.setCurrentTenant(tenant);
            ventaRepository.findById(Long.parseLong(externalRef)).ifPresent(venta -> {
                if ("approved".equals(status)) {
                    venta.setEstado("PAGADA");
                } else if ("rejected".equals(status) || "cancelled".equals(status)) {
                    venta.setEstado("PENDIENTE");
                }
                ventaRepository.save(venta);
            });

        } catch (Exception e) {
            System.err.println("Error procesando webhook MP: " + e.getMessage());
        } finally {
            TenantContext.clear();
        }

        return ResponseEntity.ok().build();
    }
}
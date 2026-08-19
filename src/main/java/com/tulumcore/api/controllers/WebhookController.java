package com.tulumcore.api.controllers;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.TenantConfig;
import com.tulumcore.api.repositories.TenantConfigRepository;
import com.tulumcore.api.repositories.VentaRepository;
import com.tulumcore.api.security.MercadoPagoWebhookValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    @Autowired private VentaRepository ventaRepository;
    @Autowired private TenantConfigRepository tenantConfigRepository;
    @Autowired private MercadoPagoWebhookValidator webhookValidator;

    /**
     * MercadoPago envía notificaciones a esta URL cuando un pago cambia de estado.
     * URL configurada en PaymentService: /api/webhook/pagos?tenant={tenantId}
     * Sin firma válida se ignora: no se marca ninguna venta.
     */
    @PostMapping("/pagos")
    public ResponseEntity<Void> recibirNotificacion(
            @RequestParam String tenant,
            @RequestParam(name = "data.id", required = false) String dataIdParam,
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestBody Map<String, Object> body) {

        String dataId = dataIdParam;
        if (dataId == null || dataId.isBlank()) {
            Object dataObj = body.get("data");
            if (dataObj instanceof Map<?, ?> data) {
                Object id = data.get("id");
                if (id != null) {
                    dataId = String.valueOf(id);
                }
            }
        }

        if (!webhookValidator.esValida(xSignature, xRequestId, dataId)) {
            return ResponseEntity.ok().build();
        }

        try {
            String tipo = (String) body.get("type");
            if (!"payment".equals(tipo)) {
                return ResponseEntity.ok().build();
            }

            Object dataObj = body.get("data");
            if (!(dataObj instanceof Map)) return ResponseEntity.ok().build();

            String paymentId = String.valueOf(((Map<?, ?>) dataObj).get("id"));

            TenantContext.setCurrentTenant(tenant);
            TenantConfig config = tenantConfigRepository.findByTenantId(tenant)
                    .orElse(null);
            if (config == null || !config.isActivo() || config.getMpAccessToken() == null) {
                return ResponseEntity.ok().build();
            }

            MercadoPagoConfig.setAccessToken(config.getMpAccessToken());

            PaymentClient paymentClient = new PaymentClient();
            Payment payment = paymentClient.get(Long.parseLong(paymentId));

            String externalRef = payment.getExternalReference();
            String status = payment.getStatus();

            if (externalRef == null) return ResponseEntity.ok().build();

            ventaRepository.findByIdAndTenantId(Long.parseLong(externalRef), tenant).ifPresent(venta -> {
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

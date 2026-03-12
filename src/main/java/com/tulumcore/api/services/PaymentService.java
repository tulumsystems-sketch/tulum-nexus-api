package com.tulumcore.api.services;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.*;
import com.mercadopago.resources.preference.Preference;
import com.tulumcore.api.entities.Venta;
import com.tulumcore.api.entities.TenantConfig;
import com.tulumcore.api.repositories.TenantConfigRepository;
import com.tulumcore.api.config.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentService {

    @Autowired private TenantConfigRepository configRepository;

    public String crearLinkDePago(Venta venta) {
        String currentTenant = TenantContext.getCurrentTenant();

        TenantConfig config = configRepository.findByTenantId(currentTenant)
                .orElseThrow(() -> new RuntimeException("Configuración de cobro no encontrada"));

        // --- DEFENSA ANTI-CRASH (Validación de Token) ---
        if (config.getMpAccessToken() == null || config.getMpAccessToken().trim().isEmpty()) {
            throw new RuntimeException("El tenant no tiene configurado un Access Token de Mercado Pago válido.");
        }

        MercadoPagoConfig.setAccessToken(config.getMpAccessToken());

        PreferenceClient client = new PreferenceClient();
        List<PreferenceItemRequest> items = new ArrayList<>();

        items.add(PreferenceItemRequest.builder()
                .id(venta.getId().toString())
                .title("Orden de Venta #" + venta.getId() + " - " + config.getNombreEmpresa())
                .quantity(1)
                .currencyId(venta.getMoneda())
                .unitPrice(new BigDecimal(venta.getTotalFinal()))
                .build());

        // Lógica de medios de pago según configuración
        List<PreferencePaymentTypeRequest> excludedTypes = new ArrayList<>();

        if (!config.isMpAceptarEfectivo()) {
            excludedTypes.add(PreferencePaymentTypeRequest.builder().id("ticket").build());
            excludedTypes.add(PreferencePaymentTypeRequest.builder().id("atm").build());
        }
        if (!config.isMpAceptarCredito()) {
            excludedTypes.add(PreferencePaymentTypeRequest.builder().id("credit_card").build());
        }
        if (!config.isMpAceptarDebito()) {
            excludedTypes.add(PreferencePaymentTypeRequest.builder().id("debit_card").build());
        }

        PreferencePaymentMethodsRequest paymentMethods = PreferencePaymentMethodsRequest.builder()
                .excludedPaymentTypes(excludedTypes)
                .installments(12)
                .build();

        PreferenceRequest request = PreferenceRequest.builder()
                .items(items)
                .paymentMethods(paymentMethods)
                .externalReference(venta.getId().toString())
                // OJO: Cuando subas a prod, cambiar el dominio base
                .notificationUrl("https://tulum-core.api/api/webhook/pagos?tenant=" + currentTenant)
                .build();

        try {
            Preference preference = client.create(request);
            return preference.getInitPoint();
        } catch (Exception e) {
            System.err.println("Error de Mercado Pago: " + e.getMessage());
            throw new RuntimeException("Error al comunicarse con Mercado Pago.");
        }
    }
}
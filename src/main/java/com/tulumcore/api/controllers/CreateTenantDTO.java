package com.tulumcore.api.controllers;

/**
 * Alta de empresa desde SuperAdmin.
 * Los feature* opcionales se persisten en tenant_features al crear.
 */
public record CreateTenantDTO(
        String tenantId,
        String nombreEmpresa,
        String adminEmail,
        String adminPassword,
        Double ivaPorcentaje,
        Boolean pagoEfectivoHabilitado,
        Boolean pagoTransferenciaHabilitado,
        Boolean pagoMercadoPagoHabilitado,
        Boolean clientesHabilitado,
        Boolean remitosHabilitado,
        Boolean comprasHabilitado,
        Boolean stockHabilitado,
        String aliasCobro,
        Double margenPorDefecto,
        Boolean featureMesas,
        Boolean featureWhatsappBot,
        Boolean featurePosBarcode
) {}

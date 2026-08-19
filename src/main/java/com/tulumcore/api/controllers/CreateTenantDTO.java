package com.tulumcore.api.controllers;

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
        Double margenPorDefecto
) {}

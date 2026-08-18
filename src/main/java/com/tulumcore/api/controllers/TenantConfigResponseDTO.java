package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.TenantConfig;

/**
 * Vista de la configuración del tenant para el frontend.
 *
 * No expone mpAccessToken: es un secreto de cobro y cualquier usuario autenticado
 * del tenant puede leer este endpoint. Se publica sólo si está configurado o no.
 */
public record TenantConfigResponseDTO(
        String nombreEmpresa,
        String logoUrl,
        boolean mpConfigurado,
        boolean mpAceptarCredito,
        boolean mpAceptarDebito,
        boolean mpAceptarEfectivo,
        boolean clientesHabilitado,
        boolean remitosHabilitado,
        boolean comprasHabilitado,
        boolean stockHabilitado,
        boolean activo,
        boolean pagoEfectivoHabilitado,
        boolean pagoTransferenciaHabilitado,
        boolean pagoMercadoPagoHabilitado,
        String aliasCobro,
        double ivaPorcentaje,
        Double margenPorDefecto
) {
    public static TenantConfigResponseDTO from(TenantConfig c) {
        String token = c.getMpAccessToken();
        return new TenantConfigResponseDTO(
                c.getNombreEmpresa(),
                c.getLogoUrl(),
                token != null && !token.isBlank(),
                c.isMpAceptarCredito(),
                c.isMpAceptarDebito(),
                c.isMpAceptarEfectivo(),
                c.isClientesHabilitado(),
                c.isRemitosHabilitado(),
                c.isComprasHabilitado(),
                c.isStockHabilitado(),
                c.isActivo(),
                c.isPagoEfectivoHabilitado(),
                c.isPagoTransferenciaHabilitado(),
                c.isPagoMercadoPagoHabilitado(),
                c.getAliasCobro(),
                c.getIvaPorcentaje(),
                c.getMargenPorDefecto()
        );
    }
}

package com.tulumcore.api.services;

import com.tulumcore.api.exceptions.BusinessException;

public final class CanalVenta {
    public static final String MOSTRADOR = "MOSTRADOR";
    public static final String WHATSAPP = "WHATSAPP";
    public static final String DELIVERY = "DELIVERY";

    private CanalVenta() {}

    public static String normalizar(String raw) {
        if (raw == null || raw.isBlank()) {
            return MOSTRADOR;
        }
        String valor = raw.trim().toUpperCase();
        if (MOSTRADOR.equals(valor) || WHATSAPP.equals(valor) || DELIVERY.equals(valor)) {
            return valor;
        }
        throw new BusinessException("Canal inválido. Usá mostrador, whatsapp o delivery.");
    }

    public static boolean esPedido(String canal) {
        return WHATSAPP.equals(canal) || DELIVERY.equals(canal);
    }
}

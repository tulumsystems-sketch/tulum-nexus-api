package com.tulumcore.api.services;

import com.tulumcore.api.exceptions.BusinessException;

import java.util.List;

public final class CanalVenta {
    public static final String MOSTRADOR = "MOSTRADOR";
    public static final String WHATSAPP = "WHATSAPP";
    public static final String DELIVERY = "DELIVERY";
    public static final String RETIRO = "RETIRO";
    public static final String SALON = "SALON";
    public static final String ECOMMERCE = "ECOMMERCE";

    private CanalVenta() {}

    public static String normalizar(String raw) {
        if (raw == null || raw.isBlank()) {
            return MOSTRADOR;
        }
        String valor = raw.trim().toUpperCase();
        if ("TAKEAWAY".equals(valor)) {
            return RETIRO;
        }
        if ("MESA".equals(valor) || "SALÓN".equals(valor) || "SALON".equals(valor)) {
            return SALON;
        }
        if (MOSTRADOR.equals(valor) || WHATSAPP.equals(valor) || DELIVERY.equals(valor)
                || RETIRO.equals(valor) || SALON.equals(valor) || ECOMMERCE.equals(valor)) {
            return valor;
        }
        throw new BusinessException("Canal inválido. Usá mostrador, whatsapp, delivery, retiro, salón o ecommerce.");
    }

    /** Pedidos externos / takeaway (tablero Pedidos). */
    public static boolean esPedido(String canal) {
        return WHATSAPP.equals(canal) || DELIVERY.equals(canal) || RETIRO.equals(canal) || ECOMMERCE.equals(canal);
    }

    public static boolean esSalon(String canal) {
        return SALON.equals(canal);
    }

    /** Cuenta que nace abierta: cocina/salón o delivery. No es cobrada de entrada. */
    public static boolean esCuentaAbierta(String canal) {
        return esPedido(canal) || esSalon(canal);
    }

    /** Delivery o WhatsApp con dirección (sale a la calle). */
    public static boolean esEnvio(String canal) {
        return DELIVERY.equals(canal);
    }

    public static boolean esEnvio(String canal, String direccionEntrega) {
        if (DELIVERY.equals(canal)) {
            return true;
        }
        if (WHATSAPP.equals(canal) && direccionEntrega != null && !direccionEntrega.isBlank()) {
            return true;
        }
        return false;
    }

    public static void exigirDireccionSiDelivery(String canal, String direccionEntrega) {
        if (DELIVERY.equals(canal) && (direccionEntrega == null || direccionEntrega.isBlank())) {
            throw new BusinessException("El delivery necesita una dirección de entrega.");
        }
    }

    public static List<String> canalesPedido() {
        return List.of(WHATSAPP, DELIVERY, RETIRO, ECOMMERCE);
    }

    /** Tablero de cocina: delivery/retiro y comandas de mesa. */
    public static List<String> canalesCocina() {
        return List.of(WHATSAPP, DELIVERY, RETIRO, ECOMMERCE, SALON);
    }
}

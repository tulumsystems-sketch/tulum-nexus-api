package com.tulumcore.api.entities;

public final class ProductoTipo {
    public static final String ELABORADO = "ELABORADO";
    public static final String INSUMO = "INSUMO";

    private ProductoTipo() {}

    public static String normalizar(String raw) {
        if (raw == null || raw.isBlank()) {
            return ELABORADO;
        }
        String valor = raw.trim().toUpperCase();
        if (INSUMO.equals(valor) || "MATERIA_PRIMA".equals(valor) || "PRIMA".equals(valor)) {
            return INSUMO;
        }
        return ELABORADO;
    }

    public static boolean esInsumo(String tipo) {
        return INSUMO.equals(normalizar(tipo));
    }
}

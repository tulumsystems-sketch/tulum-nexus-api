package com.tulumcore.api.services;

public final class TelefonoWhatsApp {
    private TelefonoWhatsApp() {}

    /**
     * Deja el número como lo manda Meta (código de país + dígitos, sin + ni 0 a la izquierda).
     * Argentina: 54911... si viene 11... o +54 9 11...
     */
    public static String normalizar(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String d = raw.replaceAll("\\D", "");
        if (d.startsWith("00")) {
            d = d.substring(2);
        }
        while (d.startsWith("0")) {
            d = d.substring(1);
        }
        if (d.isEmpty()) {
            return null;
        }
        if (d.length() >= 15) {
            return null;
        }
        if (d.startsWith("54")) {
            if (!d.startsWith("549") && d.length() == 12) {
                return "549" + d.substring(2);
            }
            return d;
        }
        if (d.startsWith("9") && d.length() >= 11) {
            return "54" + d;
        }
        if (d.length() == 10) {
            return "549" + d;
        }
        if (d.length() > 10) {
            return "54" + d;
        }
        return d;
    }

    public static boolean mismaLinea(String a, String b) {
        String na = normalizar(a);
        String nb = normalizar(b);
        if (na == null || nb == null) {
            return false;
        }
        if (na.equals(nb)) {
            return true;
        }
        if (na.length() >= 8 && nb.length() >= 8) {
            return na.substring(na.length() - 8).equals(nb.substring(nb.length() - 8));
        }
        return false;
    }
}

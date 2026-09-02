package com.tulumcore.api.services;

import com.tulumcore.api.exceptions.BusinessException;

public final class DivisionCuenta {
    private DivisionCuenta() {}

    public static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public static double saldo(double total, double yaPagado) {
        return redondear(nz(total) - nz(yaPagado));
    }

    /** Cobra una de N partes. La última se lleva el resto de centavos. */
    public static double parteActual(double total, double yaPagado, int partes, int cobradas) {
        if (partes < 2) {
            throw new BusinessException("Para dividir en partes iguales usá 2 o más.");
        }
        double restante = saldo(total, yaPagado);
        if (restante <= 0) {
            throw new BusinessException("Esa cuenta ya está cubierta con las partes cobradas.");
        }
        if (cobradas + 1 >= partes) {
            return restante;
        }
        double base = redondear(nz(total) / partes);
        return Math.min(base, restante);
    }

    public static double cobrarMonto(double yaSaldo, double pedido) {
        double restante = redondear(yaSaldo);
        if (restante <= 0) {
            throw new BusinessException("Esa cuenta ya está cubierta.");
        }
        double monto = redondear(pedido);
        if (monto <= 0) {
            throw new BusinessException("Indicá un monto mayor a cero.");
        }
        return Math.min(monto, restante);
    }

    public static boolean cubreElTotal(double total, double yaPagado) {
        return saldo(total, yaPagado) <= 0.009;
    }

    private static double nz(double valor) {
        return Double.isNaN(valor) ? 0 : valor;
    }
}

package com.tulumcore.api.services;

import com.tulumcore.api.exceptions.BusinessException;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EstadoPedido {
    public static final String PENDIENTE = "PENDIENTE";
    public static final String EN_PREPARACION = "EN_PREPARACION";
    public static final String LISTO = "LISTO";
    public static final String EN_CAMINO = "EN_CAMINO";
    public static final String ENTREGADO = "ENTREGADO";
    public static final String PAGADA = "PAGADA";
    public static final String ANULADA = "ANULADA";

    private static final Map<String, Set<String>> TRANSICIONES = Map.of(
            PENDIENTE, Set.of(EN_PREPARACION, ANULADA),
            EN_PREPARACION, Set.of(LISTO, EN_CAMINO, ANULADA),
            LISTO, Set.of(EN_CAMINO, ENTREGADO, ANULADA),
            EN_CAMINO, Set.of(ENTREGADO, ANULADA),
            ENTREGADO, Set.of(),
            PAGADA, Set.of(ANULADA)
    );

    private EstadoPedido() {}

    public static String inicialParaCanal(String canal) {
        return CanalVenta.esCuentaAbierta(canal) ? PENDIENTE : PAGADA;
    }

    public static String normalizar(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("El estado del pedido es obligatorio.");
        }
        String valor = raw.trim().toUpperCase().replace(' ', '_');
        if (!TRANSICIONES.containsKey(valor) && !ANULADA.equals(valor)) {
            throw new BusinessException("Estado inválido: " + raw);
        }
        return valor;
    }

    public static void validarTransicion(String actual, String destino) {
        String desde = actual == null || actual.isBlank() ? PENDIENTE : actual;
        String hacia = normalizar(destino);
        if (desde.equals(hacia)) {
            return;
        }
        Set<String> permitidos = TRANSICIONES.getOrDefault(desde, Set.of());
        if (!permitidos.contains(hacia)) {
            throw new BusinessException("No se puede pasar de " + desde + " a " + hacia + ".");
        }
    }

    public static List<String> siguientes(String actual, String canal) {
        return siguientes(actual, canal, null);
    }

    public static List<String> siguientes(String actual, String canal, String direccionEntrega) {
        String desde = actual == null || actual.isBlank() ? PENDIENTE : actual;
        if (ANULADA.equals(desde) || ENTREGADO.equals(desde) || PAGADA.equals(desde)) {
            return List.of();
        }
        if (CanalVenta.esSalon(canal)) {
            return switch (desde) {
                case PENDIENTE -> List.of(EN_PREPARACION);
                case EN_PREPARACION -> List.of(LISTO);
                default -> List.of();
            };
        }
        if (CanalVenta.esEnvio(canal, direccionEntrega)) {
            return switch (desde) {
                case PENDIENTE -> List.of(EN_PREPARACION);
                case EN_PREPARACION -> List.of(LISTO);
                case LISTO -> List.of(EN_CAMINO);
                case EN_CAMINO -> List.of(ENTREGADO);
                default -> List.of();
            };
        }
        return switch (desde) {
            case PENDIENTE -> List.of(EN_PREPARACION);
            case EN_PREPARACION -> List.of(LISTO);
            case LISTO, EN_CAMINO -> List.of(ENTREGADO);
            default -> List.of();
        };
    }

    public static boolean esActivo(String estado) {
        return estado != null
                && !ENTREGADO.equals(estado)
                && !ANULADA.equals(estado)
                && !PAGADA.equals(estado);
    }
}

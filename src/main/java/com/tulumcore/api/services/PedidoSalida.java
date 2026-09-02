package com.tulumcore.api.services;

import com.tulumcore.api.entities.Rol;
import com.tulumcore.api.entities.Usuario;

public final class PedidoSalida {
    private PedidoSalida() {}

    public static boolean puedeTomar(String estado, String canal, String direccionEntrega, Long repartidorUsuarioId) {
        return EstadoPedido.LISTO.equals(estado)
                && CanalVenta.esEnvio(canal, direccionEntrega)
                && repartidorUsuarioId == null;
    }

    /** Anotarse en un envío que todavía está en cocina. */
    public static boolean puedeReservar(String estado, String canal, String direccionEntrega, Long repartidorUsuarioId) {
        return repartidorUsuarioId == null
                && CanalVenta.esEnvio(canal, direccionEntrega)
                && (EstadoPedido.PENDIENTE.equals(estado) || EstadoPedido.EN_PREPARACION.equals(estado));
    }

    /** Salir a la calle: listo sin cadete, o listo ya anotado a este cadete. */
    public static boolean puedeSalir(String estado, String canal, String direccionEntrega,
                                     Long repartidorUsuarioId, Long cadeteId) {
        if (!EstadoPedido.LISTO.equals(estado) || !CanalVenta.esEnvio(canal, direccionEntrega)) {
            return false;
        }
        if (repartidorUsuarioId == null) {
            return true;
        }
        return cadeteId != null && cadeteId.equals(repartidorUsuarioId);
    }

    public static boolean visibleParaRepartidor(String estado, String canal, String direccionEntrega,
                                                Long repartidorUsuarioId, Long usuarioId) {
        if (puedeTomar(estado, canal, direccionEntrega, repartidorUsuarioId)) {
            return true;
        }
        return usuarioId != null && usuarioId.equals(repartidorUsuarioId);
    }

    public static String nombreVisible(Usuario usuario) {
        if (usuario == null) {
            return "Cadete";
        }
        String email = usuario.getEmail();
        boolean autoGenerado = email != null && email.contains(".tulum.local");
        if (email != null && !email.isBlank() && !autoGenerado) {
            int at = email.indexOf('@');
            String local = at > 0 ? email.substring(0, at) : email;
            String limpio = local.replace('.', ' ').replace('_', ' ').replace('-', ' ').trim();
            if (!limpio.isEmpty()) {
                return limpio;
            }
        }
        String tel = usuario.getTelefono();
        if (tel != null && !tel.isBlank()) {
            String cola = tel.length() > 4 ? tel.substring(tel.length() - 4) : tel;
            return "Cadete " + cola;
        }
        return "Cadete";
    }

    /** Nombre para el bot de Fogón: Socio / Caja / Delivery. */
    public static String nombreEquipo(Usuario usuario) {
        if (usuario == null) {
            return "Equipo";
        }
        String email = usuario.getEmail();
        boolean autoGenerado = email != null && email.contains(".tulum.local");
        if (email != null && !email.isBlank() && !autoGenerado) {
            int at = email.indexOf('@');
            String local = at > 0 ? email.substring(0, at) : email;
            String limpio = local.replace('.', ' ').replace('_', ' ').replace('-', ' ').trim();
            if (!limpio.isEmpty()) {
                return limpio;
            }
        }
        String etiqueta = "Equipo";
        if (usuario.getRol() == Rol.ADMIN) {
            etiqueta = "Socio";
        } else if (usuario.getRol() == Rol.OPERADOR) {
            etiqueta = "Caja";
        } else if (usuario.getRol() == Rol.REPARTIDOR) {
            etiqueta = "Delivery";
        }
        String tel = usuario.getTelefono();
        if (tel != null && !tel.isBlank()) {
            String cola = tel.length() > 4 ? tel.substring(tel.length() - 4) : tel;
            return etiqueta + " " + cola;
        }
        return etiqueta;
    }
}

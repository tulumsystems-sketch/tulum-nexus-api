package com.tulumcore.api.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tope simple anti fuerza bruta, en memoria. En un solo instancia de Railway alcanza.
 */
@Component
public class LoginAttemptService {

    static final int MAX_INTENTOS = 5;
    static final int BLOQUEO_MINUTOS = 15;

    private record Estado(int fallos, Instant bloqueadoHasta) {}

    private final ConcurrentHashMap<String, Estado> intentos = new ConcurrentHashMap<>();

    public boolean estaBloqueado(String clave) {
        Estado estado = intentos.get(clave);
        if (estado == null || estado.bloqueadoHasta() == null) {
            return false;
        }
        if (Instant.now().isAfter(estado.bloqueadoHasta())) {
            intentos.remove(clave);
            return false;
        }
        return true;
    }

    public void registrarExito(String clave) {
        intentos.remove(clave);
    }

    public void registrarFallo(String clave) {
        intentos.compute(clave, (k, actual) -> {
            int fallos = (actual == null ? 0 : actual.fallos()) + 1;
            Instant hasta = fallos >= MAX_INTENTOS
                    ? Instant.now().plusSeconds(BLOQUEO_MINUTOS * 60L)
                    : null;
            return new Estado(fallos, hasta);
        });
    }
}

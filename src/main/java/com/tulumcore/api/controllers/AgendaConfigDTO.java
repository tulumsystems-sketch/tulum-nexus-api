package com.tulumcore.api.controllers;

import java.util.List;

public record AgendaConfigDTO(
        int duracionMinutos,
        String horaInicio,
        String horaFin,
        List<Integer> diasSemana,
        String zona
) {
    public static AgendaConfigDTO porDefecto() {
        return new AgendaConfigDTO(20, "09:00", "18:00", List.of(1, 2, 3, 4, 5), "America/Argentina/Buenos_Aires");
    }
}

package com.tulumcore.api.controllers;

import java.time.Instant;

public record AgendaHuecoDTO(
        Instant inicio,
        Instant fin,
        int duracionMinutos
) {}

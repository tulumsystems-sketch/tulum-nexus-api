package com.tulumcore.api.controllers;

import java.util.List;

public record AgendaHuecosDiaDTO(
        String fecha,
        List<AgendaHuecoDTO> huecos
) {}

package com.tulumcore.api.controllers;

public record BotContextoDTO(
        String tenantId,
        String nombreEmpresa,
        boolean agenda,
        boolean mesas,
        boolean whatsappBot
) {}

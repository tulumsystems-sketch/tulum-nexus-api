package com.tulumcore.api.controllers;

public record CreateTenantDTO(
        String tenantId,
        String nombreEmpresa,
        String adminEmail,
        String adminPassword
) {}

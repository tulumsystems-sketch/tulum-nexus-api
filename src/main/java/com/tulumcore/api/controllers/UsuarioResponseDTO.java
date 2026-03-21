package com.tulumcore.api.controllers;

// DTO de respuesta — nunca exponemos el password
public record UsuarioResponseDTO(
        Long id,
        String email,
        String rol
) {}
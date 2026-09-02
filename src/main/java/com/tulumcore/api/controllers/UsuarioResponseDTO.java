package com.tulumcore.api.controllers;

public record UsuarioResponseDTO(
        Long id,
        String email,
        String rol,
        String telefono,
        String nombreVisible
) {}

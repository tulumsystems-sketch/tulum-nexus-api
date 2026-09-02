package com.tulumcore.api.controllers;

public record UsuarioCreateDTO(
        String email,
        String password,
        String rol,
        String telefono
) {}

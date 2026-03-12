package com.tulumcore.api.controllers;

// Un "record" es una clase inmutable ideal para recibir datos (DTOs) en Java moderno.
public record LoginRequestDTO(
        String email,
        String password,
        String tenant
) {}
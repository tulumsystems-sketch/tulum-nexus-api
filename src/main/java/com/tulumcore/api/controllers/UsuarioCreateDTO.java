package com.tulumcore.api.controllers;

// DTO para crear usuario — recibe email, password y rol opcional
public record UsuarioCreateDTO(
        String email,
        String password,
        String rol  // "ADMIN" o "OPERADOR" — si viene null se asigna OPERADOR
) {}
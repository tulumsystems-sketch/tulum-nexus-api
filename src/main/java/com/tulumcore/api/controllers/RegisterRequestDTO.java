package com.tulumcore.api.controllers;

public record RegisterRequestDTO(
    String tenant,
    String email,
    String password,
    String companyName
) {}

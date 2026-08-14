package com.tulumcore.api.controllers;

public record TenantFeatureUpdateDTO(
        Boolean enabled,
        String configurationJson
) {}

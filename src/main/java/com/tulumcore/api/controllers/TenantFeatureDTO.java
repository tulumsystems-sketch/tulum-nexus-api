package com.tulumcore.api.controllers;

public record TenantFeatureDTO(
        String featureKey,
        boolean enabled,
        String configurationJson
) {}

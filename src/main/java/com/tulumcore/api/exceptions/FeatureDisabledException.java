package com.tulumcore.api.exceptions;

import com.tulumcore.api.entities.FeatureKey;

public class FeatureDisabledException extends RuntimeException {
    private final FeatureKey featureKey;

    public FeatureDisabledException(FeatureKey featureKey) {
        super("La funcionalidad " + featureKey.name() + " no esta habilitada para este comercio.");
        this.featureKey = featureKey;
    }

    public FeatureKey getFeatureKey() {
        return featureKey;
    }
}

CREATE TABLE IF NOT EXISTS tenant_features (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    feature_key VARCHAR(80) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    configuration_json TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_features_tenant_feature UNIQUE (tenant_id, feature_key)
);

CREATE INDEX IF NOT EXISTS idx_tenant_features_tenant_id
    ON tenant_features (tenant_id);

CREATE INDEX IF NOT EXISTS idx_tenant_features_feature_key
    ON tenant_features (feature_key);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tenant_config_tenant_id
    ON tenant_config (tenant_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_tenant_features_tenant_config'
          AND table_name = 'tenant_features'
    ) THEN
        ALTER TABLE tenant_features
            ADD CONSTRAINT fk_tenant_features_tenant_config
            FOREIGN KEY (tenant_id)
            REFERENCES tenant_config (tenant_id);
    END IF;
END $$;

INSERT INTO tenant_features (tenant_id, feature_key, enabled, configuration_json, created_at, updated_at)
SELECT DISTINCT tenant_id, 'POS_BARCODE', TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenant_config
WHERE tenant_id IS NOT NULL
ON CONFLICT (tenant_id, feature_key) DO NOTHING;

-- Neon tiene un CHECK viejo en feature_key (sin MESAS). Lo reemplazamos
-- para alinear con el enum FeatureKey de la app.

ALTER TABLE tenant_features DROP CONSTRAINT IF EXISTS tenant_features_feature_key_check;

ALTER TABLE tenant_features
    ADD CONSTRAINT tenant_features_feature_key_check
    CHECK (feature_key IN (
        'POS_BARCODE',
        'WHATSAPP_BOT',
        'CUSTOMER_CATALOG',
        'PAYMENT_LINKS',
        'MESAS'
    ));

-- Por si V9 no alcanzó a correr o quedó a medias.
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS canal varchar(32);
UPDATE ventas SET canal = 'MOSTRADOR' WHERE canal IS NULL OR canal = '';
DO $$
BEGIN
    ALTER TABLE ventas ALTER COLUMN canal SET DEFAULT 'MOSTRADOR';
    ALTER TABLE ventas ALTER COLUMN canal SET NOT NULL;
EXCEPTION
    WHEN others THEN NULL;
END $$;

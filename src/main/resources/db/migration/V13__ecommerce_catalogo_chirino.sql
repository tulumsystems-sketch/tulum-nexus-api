-- Catálogo público (ecommerce) por producto, y flag CUSTOMER_CATALOG para Chirino.
-- Hibernate mapea la entidad Producto a "producto" (sin @Table). El SQL histórico
-- decía "productos"; en Neon esa relación no existe y tumba el migrate.

DO $$
DECLARE
    t text;
BEGIN
    IF to_regclass('public.productos') IS NOT NULL THEN
        t := 'productos';
    ELSIF to_regclass('public.producto') IS NOT NULL THEN
        t := 'producto';
    ELSE
        t := NULL;
    END IF;

    IF t IS NULL THEN
        RAISE NOTICE 'V13: no hay tabla producto/productos; se omite el ALTER de catálogo.';
        RETURN;
    END IF;

    EXECUTE format(
        'ALTER TABLE %I ADD COLUMN IF NOT EXISTS publicado_en_catalogo boolean NOT NULL DEFAULT FALSE',
        t
    );

    EXECUTE format(
        'UPDATE %I SET publicado_en_catalogo = TRUE
         WHERE tenant_id = ''chirino''
           AND COALESCE(cantidad_stock, 0) > 0',
        t
    );
END $$;

-- Chirino es el primer tenant con tienda. Si el slug no existe, este bloque no inserta nada.
INSERT INTO tenant_features (tenant_id, feature_key, enabled, configuration_json, created_at, updated_at)
SELECT tenant_id, 'CUSTOMER_CATALOG', TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenant_config
WHERE tenant_id = 'chirino'
ON CONFLICT (tenant_id, feature_key) DO UPDATE
SET enabled = TRUE,
    updated_at = CURRENT_TIMESTAMP;

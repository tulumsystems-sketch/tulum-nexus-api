-- Carta vs materia prima, recetas, y stock decimal (100 g, 0.5 kg).
-- Misma dualidad producto/productos que V13.

DO $$
DECLARE
    t text;
BEGIN
    IF to_regclass('public.productos') IS NOT NULL THEN
        t := 'productos';
    ELSIF to_regclass('public.producto') IS NOT NULL THEN
        t := 'producto';
    ELSE
        RAISE NOTICE 'V14: no hay tabla producto/productos; se omite el ALTER de recetas.';
        RETURN;
    END IF;

    EXECUTE format(
        'ALTER TABLE %I ADD COLUMN IF NOT EXISTS tipo varchar(20) NOT NULL DEFAULT ''ELABORADO''',
        t
    );

    EXECUTE format(
        'ALTER TABLE %I ALTER COLUMN cantidad_stock TYPE numeric(14, 3) USING COALESCE(cantidad_stock, 0)::numeric',
        t
    );

    IF to_regclass('public.stock_movements') IS NOT NULL THEN
        ALTER TABLE stock_movements
            ALTER COLUMN cantidad TYPE numeric(14, 3)
            USING COALESCE(cantidad, 0)::numeric;
    END IF;

    EXECUTE format(
        'CREATE TABLE IF NOT EXISTS producto_receta (
            id bigserial PRIMARY KEY,
            tenant_id varchar(64) NOT NULL,
            producto_id bigint NOT NULL REFERENCES %I (id) ON DELETE CASCADE,
            insumo_id bigint NOT NULL REFERENCES %I (id),
            cantidad numeric(14, 3) NOT NULL,
            CONSTRAINT producto_receta_unica UNIQUE (producto_id, insumo_id),
            CONSTRAINT producto_receta_cantidad_chk CHECK (cantidad > 0),
            CONSTRAINT producto_receta_distintos_chk CHECK (producto_id <> insumo_id)
        )',
        t, t
    );

    CREATE INDEX IF NOT EXISTS idx_producto_receta_tenant ON producto_receta (tenant_id);
    CREATE INDEX IF NOT EXISTS idx_producto_receta_producto ON producto_receta (producto_id);
END $$;

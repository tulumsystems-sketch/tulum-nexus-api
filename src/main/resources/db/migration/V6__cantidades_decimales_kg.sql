ALTER TABLE remito_items
    ALTER COLUMN cantidad TYPE double precision USING cantidad::double precision;

DO $$
BEGIN
    IF to_regclass('public.productos') IS NOT NULL THEN
        ALTER TABLE productos
            ALTER COLUMN cantidad_stock TYPE double precision USING cantidad_stock::double precision;
    ELSIF to_regclass('public.producto') IS NOT NULL THEN
        ALTER TABLE producto
            ALTER COLUMN cantidad_stock TYPE double precision USING cantidad_stock::double precision;
    END IF;
END $$;

ALTER TABLE stock_movements
    ALTER COLUMN cantidad TYPE double precision USING cantidad::double precision;

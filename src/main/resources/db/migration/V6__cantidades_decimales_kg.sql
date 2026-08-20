ALTER TABLE remito_items
    ALTER COLUMN cantidad TYPE double precision USING cantidad::double precision;

ALTER TABLE productos
    ALTER COLUMN cantidad_stock TYPE double precision USING cantidad_stock::double precision;

ALTER TABLE stock_movements
    ALTER COLUMN cantidad TYPE double precision USING cantidad::double precision;

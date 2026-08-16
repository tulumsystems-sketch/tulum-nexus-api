ALTER TABLE remito_items
    ADD COLUMN IF NOT EXISTS precio_unitario DOUBLE PRECISION NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_linea DOUBLE PRECISION NOT NULL DEFAULT 0;

ALTER TABLE remitos
    ADD COLUMN IF NOT EXISTS total DOUBLE PRECISION NOT NULL DEFAULT 0;

UPDATE remito_items ri
SET precio_unitario = COALESCE(p.precio, 0),
    total_linea = COALESCE(p.precio, 0) * COALESCE(ri.cantidad, 0)
FROM productos p
WHERE ri.producto_id = p.id
  AND COALESCE(ri.precio_unitario, 0) = 0
  AND COALESCE(ri.total_linea, 0) = 0;

UPDATE remitos r
SET total = totals.total
FROM (
    SELECT remito_id, SUM(COALESCE(total_linea, 0)) AS total
    FROM remito_items
    GROUP BY remito_id
) totals
WHERE r.id = totals.remito_id
  AND COALESCE(r.total, 0) = 0;

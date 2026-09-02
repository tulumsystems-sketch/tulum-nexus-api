-- Cobro independiente del estado de cocina, y notas por ítem.
-- Las ventas ya existentes se marcan cobradas para no desarmar la caja.

ALTER TABLE ventas ADD COLUMN IF NOT EXISTS cobrado boolean;

UPDATE ventas SET cobrado = true WHERE cobrado IS NULL;

ALTER TABLE ventas ALTER COLUMN cobrado SET DEFAULT false;
ALTER TABLE ventas ALTER COLUMN cobrado SET NOT NULL;

ALTER TABLE venta_items ADD COLUMN IF NOT EXISTS observaciones varchar(255);

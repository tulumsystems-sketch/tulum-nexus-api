-- Partes cobradas de una cuenta de salón (dividir la mesa).
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS venta_origen_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_ventas_origen_id ON ventas (venta_origen_id);

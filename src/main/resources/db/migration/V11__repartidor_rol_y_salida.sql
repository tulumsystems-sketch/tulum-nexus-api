-- Cadete Tulum: rol REPARTIDOR + quién tomó cada envío.
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_rol_check;
ALTER TABLE usuarios ADD CONSTRAINT usuarios_rol_check
    CHECK (rol IN ('SUPER_ADMIN', 'ADMIN', 'OPERADOR', 'PREVENTISTA', 'REPARTIDOR'));

ALTER TABLE ventas ADD COLUMN IF NOT EXISTS repartidor_nombre varchar(255);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS repartidor_usuario_id bigint;

CREATE INDEX IF NOT EXISTS idx_ventas_salida
    ON ventas (tenant_id, estado, canal, repartidor_usuario_id);

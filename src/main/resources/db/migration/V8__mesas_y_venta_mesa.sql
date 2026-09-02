-- Mesas de salón y vínculo a la venta (cuenta abierta).

CREATE TABLE IF NOT EXISTS mesas (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    numero INTEGER NOT NULL,
    nombre VARCHAR(120),
    capacidad INTEGER,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    estado VARCHAR(32) NOT NULL DEFAULT 'LIBRE',
    CONSTRAINT uk_mesas_tenant_numero UNIQUE (tenant_id, numero)
);

CREATE INDEX IF NOT EXISTS idx_mesas_tenant_id ON mesas (tenant_id);
CREATE INDEX IF NOT EXISTS idx_mesas_tenant_estado ON mesas (tenant_id, estado);

ALTER TABLE ventas ADD COLUMN IF NOT EXISTS mesa_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_ventas_mesa'
          AND table_name = 'ventas'
    ) THEN
        ALTER TABLE ventas
            ADD CONSTRAINT fk_ventas_mesa
            FOREIGN KEY (mesa_id) REFERENCES mesas (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_ventas_mesa_id ON ventas (mesa_id);

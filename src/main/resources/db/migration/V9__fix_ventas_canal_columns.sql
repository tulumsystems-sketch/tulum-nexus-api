-- V6 quedó registrada en flyway_schema_history (repair de checksum) sin haber
-- creado las columnas en Neon. Reaplicamos de forma idempotente.

ALTER TABLE ventas ADD COLUMN IF NOT EXISTS canal varchar(32);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS nombre_contacto varchar(255);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS telefono_contacto varchar(64);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS direccion_entrega varchar(500);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS cobrado boolean;
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS mesa_id BIGINT;

UPDATE ventas SET canal = 'MOSTRADOR' WHERE canal IS NULL OR canal = '';
UPDATE ventas SET cobrado = true WHERE cobrado IS NULL;

UPDATE ventas
SET canal = 'WHATSAPP'
WHERE canal = 'MOSTRADOR'
  AND (
        lower(coalesce(observaciones, '')) LIKE '%pedido automático vía whatsapp%'
     OR lower(coalesce(observaciones, '')) LIKE '%pedido automatico via whatsapp%'
  );

DO $$
BEGIN
    ALTER TABLE ventas ALTER COLUMN canal SET DEFAULT 'MOSTRADOR';
    ALTER TABLE ventas ALTER COLUMN canal SET NOT NULL;
EXCEPTION
    WHEN others THEN NULL;
END $$;

DO $$
BEGIN
    ALTER TABLE ventas ALTER COLUMN cobrado SET DEFAULT false;
    ALTER TABLE ventas ALTER COLUMN cobrado SET NOT NULL;
EXCEPTION
    WHEN others THEN NULL;
END $$;

ALTER TABLE venta_items ADD COLUMN IF NOT EXISTS observaciones varchar(255);

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

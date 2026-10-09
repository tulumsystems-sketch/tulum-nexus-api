-- Vertical consultorio: feature AGENDA, ficha administrativa de paciente, turnos.

ALTER TABLE tenant_features DROP CONSTRAINT IF EXISTS tenant_features_feature_key_check;

ALTER TABLE tenant_features
    ADD CONSTRAINT tenant_features_feature_key_check
    CHECK (feature_key IN (
        'POS_BARCODE',
        'WHATSAPP_BOT',
        'CUSTOMER_CATALOG',
        'PAYMENT_LINKS',
        'MESAS',
        'AGENDA'
    ));

ALTER TABLE clientes ADD COLUMN IF NOT EXISTS dni varchar(32);
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS obra_social varchar(120);
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS nro_afiliado varchar(64);
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS nota_secretaria text;

CREATE INDEX IF NOT EXISTS idx_clientes_tenant_dni ON clientes (tenant_id, dni);
CREATE INDEX IF NOT EXISTS idx_clientes_tenant_telefono ON clientes (tenant_id, telefono);

CREATE TABLE IF NOT EXISTS turnos (
    id bigserial PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    cliente_id bigint NOT NULL REFERENCES clientes (id),
    producto_id bigint,
    venta_id bigint,
    inicio timestamptz NOT NULL,
    fin timestamptz NOT NULL,
    duracion_minutos integer NOT NULL DEFAULT 20,
    estado varchar(24) NOT NULL DEFAULT 'PEDIDO',
    modalidad varchar(24) NOT NULL DEFAULT 'PARTICULAR',
    notas text,
    recordatorio_24h boolean NOT NULL DEFAULT FALSE,
    recordatorio_2h boolean NOT NULL DEFAULT FALSE,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT turnos_estado_chk CHECK (estado IN (
        'PEDIDO', 'CONFIRMADO', 'EN_ESPERA', 'ATENDIDO', 'AUSENTE', 'CANCELADO'
    )),
    CONSTRAINT turnos_modalidad_chk CHECK (modalidad IN ('PARTICULAR', 'OBRA_SOCIAL')),
    CONSTRAINT turnos_rango_chk CHECK (fin > inicio)
);

CREATE INDEX IF NOT EXISTS idx_turnos_tenant_inicio ON turnos (tenant_id, inicio);
CREATE INDEX IF NOT EXISTS idx_turnos_tenant_estado ON turnos (tenant_id, estado);
CREATE INDEX IF NOT EXISTS idx_turnos_cliente ON turnos (cliente_id);

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

    IF t IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'turnos_producto_fk'
    ) THEN
        EXECUTE format(
            'ALTER TABLE turnos ADD CONSTRAINT turnos_producto_fk FOREIGN KEY (producto_id) REFERENCES %I (id)',
            t
        );
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('public.ventas') IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'turnos_venta_fk'
    ) THEN
        ALTER TABLE turnos ADD CONSTRAINT turnos_venta_fk FOREIGN KEY (venta_id) REFERENCES ventas (id);
    END IF;
END $$;

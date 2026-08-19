ALTER TABLE cajas ADD COLUMN IF NOT EXISTS cierre_automatico BOOLEAN DEFAULT FALSE;
ALTER TABLE cajas ADD COLUMN IF NOT EXISTS motivo_cierre VARCHAR(500);

UPDATE cajas SET cierre_automatico = FALSE WHERE cierre_automatico IS NULL;

CREATE TABLE IF NOT EXISTS caja_descargos (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    caja_id BIGINT NOT NULL REFERENCES cajas(id),
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    monto_anterior DOUBLE PRECISION,
    monto_nuevo DOUBLE PRECISION,
    diferencia DOUBLE PRECISION,
    motivo VARCHAR(500) NOT NULL,
    usuario_id BIGINT REFERENCES usuarios(id)
);

CREATE INDEX IF NOT EXISTS idx_caja_descargos_caja_id ON caja_descargos (caja_id);
CREATE INDEX IF NOT EXISTS idx_caja_descargos_tenant_id ON caja_descargos (tenant_id);

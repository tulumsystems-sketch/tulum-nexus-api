-- El cadete se vincula por WhatsApp, no por login.
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS telefono varchar(32);

CREATE UNIQUE INDEX IF NOT EXISTS idx_usuarios_tenant_telefono
    ON usuarios (tenant_id, telefono)
    WHERE telefono IS NOT NULL AND telefono <> '';

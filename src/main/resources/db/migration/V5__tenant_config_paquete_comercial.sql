ALTER TABLE tenant_config ADD COLUMN IF NOT EXISTS pago_efectivo_habilitado boolean DEFAULT true;
ALTER TABLE tenant_config ADD COLUMN IF NOT EXISTS pago_transferencia_habilitado boolean DEFAULT false;
ALTER TABLE tenant_config ADD COLUMN IF NOT EXISTS pago_mercado_pago_habilitado boolean DEFAULT true;
ALTER TABLE tenant_config ADD COLUMN IF NOT EXISTS alias_cobro varchar(255);
ALTER TABLE tenant_config ADD COLUMN IF NOT EXISTS iva_porcentaje double precision DEFAULT 21;
ALTER TABLE tenant_config ADD COLUMN IF NOT EXISTS margen_por_defecto double precision;

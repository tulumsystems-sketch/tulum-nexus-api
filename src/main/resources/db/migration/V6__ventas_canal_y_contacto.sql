-- Canal de venta explícito y datos de contacto para delivery / WhatsApp.
-- Las ventas viejas quedan en mostrador; las que ya venían del bot se marcan whatsapp.

ALTER TABLE ventas ADD COLUMN IF NOT EXISTS canal varchar(32);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS nombre_contacto varchar(255);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS telefono_contacto varchar(64);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS direccion_entrega varchar(500);

UPDATE ventas SET canal = 'MOSTRADOR' WHERE canal IS NULL OR canal = '';

UPDATE ventas
SET canal = 'WHATSAPP'
WHERE lower(coalesce(observaciones, '')) LIKE '%pedido automático vía whatsapp%'
   OR lower(coalesce(observaciones, '')) LIKE '%pedido automatico via whatsapp%';

ALTER TABLE ventas ALTER COLUMN canal SET DEFAULT 'MOSTRADOR';
ALTER TABLE ventas ALTER COLUMN canal SET NOT NULL;

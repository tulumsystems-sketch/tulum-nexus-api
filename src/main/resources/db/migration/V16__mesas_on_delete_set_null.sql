-- Borrar una mesa no debe fallar por tickets ya cobrados/anulados.
-- El historial se conserva; mesa_id queda null.

ALTER TABLE ventas DROP CONSTRAINT IF EXISTS fk_ventas_mesa;

ALTER TABLE ventas
    ADD CONSTRAINT fk_ventas_mesa
    FOREIGN KEY (mesa_id) REFERENCES mesas (id)
    ON DELETE SET NULL;

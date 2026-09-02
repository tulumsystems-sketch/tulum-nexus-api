-- Carta vs depósito: un artículo de stock puede venderse (bebida)
-- sin ser un plato con receta. El fiambre sigue siendo solo insumo.
-- Misma dualidad producto/productos que V13/V14.

DO $$
DECLARE
    t text;
BEGIN
    IF to_regclass('public.productos') IS NOT NULL THEN
        t := 'productos';
    ELSIF to_regclass('public.producto') IS NOT NULL THEN
        t := 'producto';
    ELSE
        RAISE NOTICE 'V17: no hay tabla producto/productos; se omite vendible.';
        RETURN;
    END IF;

    EXECUTE format(
        'ALTER TABLE %I ADD COLUMN IF NOT EXISTS vendible boolean NOT NULL DEFAULT TRUE',
        t
    );

    -- Materia prima ya cargada (fiambre, limpieza) no sale en la carta.
    -- Las bebidas creadas como plato (ELABORADO) siguen vendibles.
    EXECUTE format(
        'UPDATE %I SET vendible = FALSE WHERE UPPER(COALESCE(tipo, '''')) = ''INSUMO''',
        t
    );
END $$;

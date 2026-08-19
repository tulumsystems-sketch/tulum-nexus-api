package com.tulumcore.api.controllers;

/**
 * Catálogo que ve el bot: precio de venta, sin costo ni margen.
 */
public record BotProductoDTO(
        Long id,
        String nombre,
        String descripcion,
        Double precio,
        Integer cantidadStock,
        String medidas,
        String imageUrl
) {}

package com.tulumcore.api.controllers;

/**
 * Catálogo que ve el bot: precio de venta, sin costo ni margen.
 */
public record BotProductoDTO(
        Long id,
        String nombre,
        String descripcion,
        Double precio,
        Double cantidadStock,
        String medidas,
        String imageUrl,
        Double disponible,
        String categoria
) {
    public BotProductoDTO(
            Long id,
            String nombre,
            String descripcion,
            Double precio,
            Double cantidadStock,
            String medidas,
            String imageUrl
    ) {
        this(id, nombre, descripcion, precio, cantidadStock, medidas, imageUrl, cantidadStock, null);
    }
}

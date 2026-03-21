package com.tulumcore.api.controllers;

/**
 * DTO de respuesta para Producto.
 * Controlamos exactamente qué campos exponemos a la API.
 */
public record ProductoResponseDTO(
        Long id,
        String nombre,
        String descripcion,
        Double precio,
        Integer cantidadStock,
        String medidas,
        String imageUrl,
        CategoriaDTO categoria
) {}
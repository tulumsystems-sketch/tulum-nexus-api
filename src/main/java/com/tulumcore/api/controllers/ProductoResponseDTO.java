package com.tulumcore.api.controllers;

import java.util.List;

public record ProductoResponseDTO(
        Long id,
        String nombre,
        String descripcion,
        Double precio,
        Double precioCosto,
        Double margenPorcentaje,
        Double cantidadStock,
        Integer stockMinimo,
        String medidas,
        String codigoBarras,
        String imageUrl,
        boolean publicadoEnCatalogo,
        String tipo,
        boolean vendible,
        Double porcionesEstimadas,
        List<RecetaLineaDTO> receta,
        CategoriaDTO categoria
) {}

package com.tulumcore.api.controllers;

import java.util.List;

public record StorefrontDTO(
        String tenantId,
        String nombre,
        String logoUrl,
        String aliasCobro,
        double ivaPorcentaje,
        boolean pagoEfectivo,
        boolean pagoTransferencia,
        List<StoreCategoriaDTO> categorias,
        List<StoreProductoDTO> productos
) {
    public record StoreCategoriaDTO(Long id, String nombre) {}

    public record StoreProductoDTO(
            Long id,
            String nombre,
            String descripcion,
            Double precio,
            Double stock,
            String medidas,
            String imageUrl,
            Long categoriaId,
            String categoriaNombre
    ) {}
}

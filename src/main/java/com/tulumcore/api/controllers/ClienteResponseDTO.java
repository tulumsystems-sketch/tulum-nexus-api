package com.tulumcore.api.controllers;

/**
 * DTO de respuesta para Cliente.
 * Solo exponemos los campos que el frontend necesita.
 * Cualquier campo nuevo en la entidad NO se expone automáticamente.
 */
public record ClienteResponseDTO(
        Long id,
        String nombre,
        String apellido,
        String empresa,
        String telefono,
        String direccion,
        String googleMapsUrl,
        Double saldoCuentaCorriente
) {
    // Los Records en Java generan automáticamente constructor, getters, equals y hashCode.
    // No necesitás escribir nada más.
}
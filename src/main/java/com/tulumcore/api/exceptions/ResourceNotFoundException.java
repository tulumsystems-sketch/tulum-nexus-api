package com.tulumcore.api.exceptions;

// =============================================
// Recurso no encontrado — 404
// Uso: throw new ResourceNotFoundException("Producto no encontrado")
// =============================================
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
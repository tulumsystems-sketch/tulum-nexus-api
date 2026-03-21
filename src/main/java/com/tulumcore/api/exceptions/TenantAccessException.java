package com.tulumcore.api.exceptions;

// =============================================
// Acceso a datos de otro tenant — 403
// Uso: throw new TenantAccessException("Acceso denegado")
// =============================================
public class TenantAccessException extends RuntimeException {
    public TenantAccessException(String message) {
        super(message);
    }
}
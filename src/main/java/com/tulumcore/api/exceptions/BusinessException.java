package com.tulumcore.api.exceptions;

// =============================================
// Regla de negocio violada — 400
// Uso: throw new BusinessException("Stock insuficiente para: Cemento")
//      throw new BusinessException("Ya existe una caja abierta")
// =============================================
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
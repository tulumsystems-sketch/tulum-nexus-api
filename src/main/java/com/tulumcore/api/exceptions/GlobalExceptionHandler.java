package com.tulumcore.api.exceptions;

import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(TenantAccessException.class)
    public ResponseEntity<Map<String, Object>> handleTenantAccess(TenantAccessException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(FeatureDisabledException.class)
    public ResponseEntity<Map<String, Object>> handleFeatureDisabled(FeatureDisabledException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", HttpStatus.FORBIDDEN.getReasonPhrase());
        body.put("code", "FEATURE_DISABLED");
        body.put("feature", ex.getFeatureKey().name());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    /**
     * El navegador o Vite abortó el GET (refresh, cambio de pestaña, timeout).
     * No es un fallo de negocio: no hay a quién responderle.
     */
    @ExceptionHandler({AsyncRequestNotUsableException.class, ClientAbortException.class})
    public void handleClientAbort(Exception ex) {
        log.debug("Cliente cerró la conexión antes de terminar la respuesta: {}", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        if (esAbortoDeCliente(ex)) {
            log.debug("Cliente cerró la conexión antes de terminar la respuesta: {}", ex.getMessage());
            return null;
        }
        log.error("Error interno no controlado", ex);
        String detalle = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, detalle);
    }

    private boolean esAbortoDeCliente(Throwable ex) {
        Throwable actual = ex;
        while (actual != null) {
            if (actual instanceof ClientAbortException
                    || actual instanceof AsyncRequestNotUsableException
                    || (actual instanceof IOException && mensajeDeConexionCerrada(actual.getMessage()))) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }

    private boolean mensajeDeConexionCerrada(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("anulado una conexión")
                || lower.contains("broken pipe")
                || lower.contains("connection reset")
                || lower.contains("connection was aborted");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}

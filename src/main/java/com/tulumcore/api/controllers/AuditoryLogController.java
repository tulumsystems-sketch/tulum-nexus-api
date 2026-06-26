package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.AuditoryLog;
import com.tulumcore.api.services.AuditoryLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit-log")
public class AuditoryLogController {

    @Autowired
    private AuditoryLogService service;

    @GetMapping
    public ResponseEntity<List<AuditoryLog>> listar(
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {

        if (entidad != null || accion != null || desde != null || hasta != null) {
            return ResponseEntity.ok(service.buscarPorFiltros(entidad, accion, desde, hasta));
        }
        return ResponseEntity.ok(service.listar());
    }
}

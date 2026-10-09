package com.tulumcore.api.controllers;

import com.tulumcore.api.services.AgendaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/agenda")
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    @GetMapping("/config")
    public AgendaConfigDTO config() {
        return agendaService.leerConfig();
    }

    @PutMapping("/config")
    public AgendaConfigDTO guardarConfig(@RequestBody AgendaConfigDTO dto) {
        return agendaService.guardarConfig(dto);
    }

    @GetMapping("/turnos")
    public List<TurnoListadoDTO> turnos(
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return agendaService.listar(desde, hasta);
    }

    @GetMapping("/turnos/{id}")
    public TurnoListadoDTO obtener(@PathVariable Long id) {
        return agendaService.obtener(id);
    }

    @PostMapping("/turnos")
    public TurnoListadoDTO crear(@RequestBody TurnoDTO dto) {
        return agendaService.crear(dto, true);
    }

    @PutMapping("/turnos/{id}")
    public TurnoListadoDTO actualizar(@PathVariable Long id, @RequestBody TurnoDTO dto) {
        return agendaService.actualizar(id, dto);
    }

    @PutMapping("/turnos/{id}/estado")
    public TurnoListadoDTO estado(@PathVariable Long id, @RequestBody TurnoEstadoDTO dto) {
        return agendaService.cambiarEstado(id, dto != null ? dto.estado() : null);
    }

    @PostMapping("/turnos/{id}/cobrar")
    public TurnoListadoDTO cobrar(@PathVariable Long id, @RequestBody(required = false) TurnoDTO dto) {
        return agendaService.cobrar(id, dto != null ? dto.getMetodoPago() : null);
    }

    @GetMapping("/huecos")
    public AgendaHuecosDiaDTO huecos(
            @RequestParam(value = "fecha", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        return agendaService.huecos(fecha);
    }

    @GetMapping("/huecos/proximos")
    public List<AgendaHuecosDiaDTO> proximos(@RequestParam(value = "dias", defaultValue = "7") int dias) {
        return agendaService.huecosProximos(dias);
    }

    @GetMapping("/contexto")
    public BotContextoDTO contexto() {
        return agendaService.contexto();
    }

    @DeleteMapping("/turnos/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        agendaService.cambiarEstado(id, "CANCELADO");
        return ResponseEntity.noContent().build();
    }
}

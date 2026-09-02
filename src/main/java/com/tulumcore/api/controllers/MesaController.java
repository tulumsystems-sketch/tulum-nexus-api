package com.tulumcore.api.controllers;

import com.tulumcore.api.services.MesaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mesas")
public class MesaController {

    @Autowired
    private MesaService mesaService;

    @GetMapping
    public List<MesaListadoDTO> listar() {
        return mesaService.listar();
    }

    @GetMapping("/{id}")
    public MesaListadoDTO obtener(@PathVariable Long id) {
        return mesaService.obtener(id);
    }

    @PostMapping
    public MesaListadoDTO crear(@RequestBody MesaDTO dto) {
        return mesaService.crear(dto);
    }

    @PostMapping("/lote")
    public List<MesaListadoDTO> crearLote(@RequestBody MesaLoteDTO dto) {
        return mesaService.crearLote(dto);
    }

    @PutMapping("/{id}")
    public MesaListadoDTO actualizar(@PathVariable Long id, @RequestBody MesaDTO dto) {
        return mesaService.actualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        mesaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/abrir")
    public MesaListadoDTO abrir(@PathVariable Long id) {
        return mesaService.abrirCuenta(id);
    }

    @GetMapping("/{id}/cuenta")
    public MesaCuentaDTO cuenta(@PathVariable Long id) {
        return mesaService.obtenerCuenta(id);
    }

    @PutMapping("/{id}/cuenta")
    public MesaCuentaDTO actualizarCuenta(@PathVariable Long id, @RequestBody VentaDTO dto) {
        return mesaService.actualizarCuenta(id, dto);
    }

    @PostMapping("/{id}/cobrar")
    public MesaCuentaDTO cobrar(@PathVariable Long id, @RequestBody(required = false) VentaCobroDTO dto) {
        return mesaService.cobrar(id, dto);
    }

    @PostMapping("/{id}/anular")
    public MesaCuentaDTO anular(@PathVariable Long id) {
        return mesaService.anularCuenta(id);
    }

    @PostMapping("/{id}/pasar")
    public MesaCuentaDTO pasar(@PathVariable Long id, @RequestBody MesaTraspasoDTO dto) {
        return mesaService.pasar(id, dto);
    }

    @PostMapping("/{id}/juntar")
    public MesaCuentaDTO juntar(@PathVariable Long id, @RequestBody MesaTraspasoDTO dto) {
        return mesaService.juntar(id, dto);
    }

    @PostMapping("/{id}/dividir")
    public MesaCuentaDTO dividir(@PathVariable Long id, @RequestBody MesaDivisionDTO dto) {
        return mesaService.dividir(id, dto);
    }
}

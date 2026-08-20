package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.Remito;
import com.tulumcore.api.services.RemitoPdfService;
import com.tulumcore.api.services.RemitoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/remitos")
public class RemitoController {

    @Autowired
    private RemitoService remitoService;

    @Autowired
    private RemitoPdfService remitoPdfService;

    @GetMapping
    public List<Remito> getAll() {
        return remitoService.getAll();
    }

    @GetMapping("/cobranzas/resumen")
    public ResponseEntity<ResumenCobranzasDTO> getResumenCobranzas() {
        return ResponseEntity.ok(remitoService.getResumenCobranzas());
    }

    @GetMapping("/estado/{estado}")
    public List<Remito> getByEstado(@PathVariable String estado) {
        return remitoService.getByEstado(estado);
    }

    @GetMapping("/estado-pago/{estadoPago}")
    public List<Remito> getByEstadoPago(@PathVariable String estadoPago) {
        return remitoService.getByEstadoPago(estadoPago);
    }

    @PostMapping
    public ResponseEntity<Remito> crear(@RequestBody RemitoDTO dto) {
        return ResponseEntity.ok(remitoService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Remito> actualizar(@PathVariable Long id, @RequestBody RemitoDTO dto) {
        return ResponseEntity.ok(remitoService.actualizar(id, dto));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<Remito> cambiarEstado(
            @PathVariable Long id,
            @RequestParam String estado) {
        return ResponseEntity.ok(remitoService.cambiarEstado(id, estado));
    }

    /** Descarga el remito en PDF con los datos del negocio, los articulos y el estado de cobranza. */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) throws Exception {
        Remito remito = remitoService.getById(id);
        byte[] pdf = remitoPdfService.generarRemitoPDF(remito);

        String nombreArchivo = "remito-"
                + (remito.getNroRemito() != null ? remito.getNroRemito() : String.valueOf(remito.getId()))
                + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .body(pdf);
    }

    /** Registra una cobranza total o parcial sobre el remito y devuelve el remito actualizado. */
    @PostMapping("/{id}/pagos")
    public ResponseEntity<Remito> registrarPago(@PathVariable Long id, @RequestBody PagoRemitoDTO dto) {
        return ResponseEntity.ok(remitoService.registrarPago(id, dto));
    }

    @GetMapping("/{id}/pagos")
    public ResponseEntity<List<PagoRemitoResponseDTO>> getPagos(@PathVariable Long id) {
        return ResponseEntity.ok(remitoService.getPagos(id));
    }
}

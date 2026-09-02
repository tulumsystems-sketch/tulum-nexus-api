package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.FeatureKey;
import com.tulumcore.api.entities.Venta;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.services.BotOperacionService;
import com.tulumcore.api.services.CanalVenta;
import com.tulumcore.api.services.TelefonoWhatsApp;
import com.tulumcore.api.services.TenantFeatureService;
import com.tulumcore.api.services.VentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/external/bot")
public class ExternalBotController {

    @Autowired private VentaService ventaService;
    @Autowired private TenantFeatureService tenantFeatureService;
    @Autowired private BotOperacionService botOperacionService;

    @GetMapping("/buscar")
    public ResponseEntity<List<BotProductoDTO>> buscarParaBot(@RequestParam("q") String query) {
        tenantFeatureService.requireEnabled(FeatureKey.WHATSAPP_BOT);
        return ResponseEntity.ok(botOperacionService.buscarPublico(query));
    }

    @GetMapping("/carta")
    public ResponseEntity<List<BotProductoDTO>> carta() {
        tenantFeatureService.requireEnabled(FeatureKey.WHATSAPP_BOT);
        return ResponseEntity.ok(botOperacionService.cartaPublica());
    }

    @GetMapping("/mi-pedido")
    public ResponseEntity<VentaListadoDTO> miPedido(@RequestParam("telefono") String telefono) {
        tenantFeatureService.requireEnabled(FeatureKey.WHATSAPP_BOT);
        Venta venta = ventaService.obtenerPedidoPorTelefono(telefono);
        if (venta == null) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.ok(ventaService.toListado(ventaService.obtenerDetalle(venta.getId())));
    }

    @PostMapping("/pedido")
    public ResponseEntity<PedidoCreadoDTO> crearPedidoDesdeBot(@RequestBody ExternalOrderDTO dto) {
        tenantFeatureService.requireEnabled(FeatureKey.WHATSAPP_BOT);
        if (dto == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException("El pedido necesita al menos un producto.");
        }

        VentaDTO ventaDto = new VentaDTO();
        ventaDto.setCanal(CanalVenta.WHATSAPP);
        ventaDto.setMetodoPago(dto.getMetodoPago());
        ventaDto.setTelefonoContacto(TelefonoWhatsApp.normalizar(primerTexto(dto.getClienteTelefono())));
        ventaDto.setNombreContacto(primerTexto(dto.getNombre()));
        ventaDto.setDireccionEntrega(primerTexto(dto.getDireccion()));
        ventaDto.setObservaciones(armarObservaciones(dto));

        List<ItemVentaDTO> itemsVenta = new ArrayList<>();
        for (ExternalOrderDTO.ItemBotDTO itemBot : dto.getItems()) {
            ItemVentaDTO iv = new ItemVentaDTO();
            iv.setProductoId(itemBot.getProductoId());
            iv.setCantidad(itemBot.getCantidad());
            itemsVenta.add(iv);
        }
        ventaDto.setItems(itemsVenta);

        Venta resultado = ventaService.guardar(ventaDto);
        return ResponseEntity.ok(PedidoCreadoDTO.desde(resultado));
    }

    @GetMapping("/cadetes")
    public ResponseEntity<List<BotCadeteDTO>> cadetes() {
        tenantFeatureService.requireEnabled(FeatureKey.WHATSAPP_BOT);
        return ResponseEntity.ok(ventaService.listarCadetesWhatsApp());
    }

    @GetMapping("/duenos")
    public ResponseEntity<List<BotCadeteDTO>> duenos() {
        tenantFeatureService.requireEnabled(FeatureKey.WHATSAPP_BOT);
        return ResponseEntity.ok(ventaService.listarDuenosWhatsApp());
    }

    @GetMapping("/socios")
    public ResponseEntity<List<BotCadeteDTO>> socios() {
        return duenos();
    }

    @GetMapping("/cajas")
    public ResponseEntity<List<BotCadeteDTO>> cajas() {
        tenantFeatureService.requireEnabled(FeatureKey.WHATSAPP_BOT);
        return ResponseEntity.ok(ventaService.listarCajasWhatsApp());
    }

    @GetMapping("/equipo")
    public ResponseEntity<BotEquipoDTO> equipo() {
        tenantFeatureService.requireEnabled(FeatureKey.WHATSAPP_BOT);
        return ResponseEntity.ok(ventaService.listarEquipoWhatsApp());
    }

    @GetMapping("/operacion")
    public ResponseEntity<BotOperacionDTO> operacion(@RequestParam("telefono") String telefono) {
        tenantFeatureService.requireEnabled(FeatureKey.WHATSAPP_BOT);
        return ResponseEntity.ok(botOperacionService.operacion(telefono));
    }

    @GetMapping("/cobro")
    public ResponseEntity<java.util.Map<String, String>> cobro() {
        tenantFeatureService.requireEnabled(FeatureKey.WHATSAPP_BOT);
        return ResponseEntity.ok(botOperacionService.cobroPublico());
    }

    @GetMapping("/envios")
    public ResponseEntity<BotEnviosDTO> envios() {
        tenantFeatureService.requireEnabled(FeatureKey.WHATSAPP_BOT);
        return ResponseEntity.ok(ventaService.obtenerEnviosParaBot());
    }

    @PostMapping("/tomar")
    public ResponseEntity<VentaListadoDTO> tomar(@RequestBody BotCadeteAccionDTO dto) {
        tenantFeatureService.requireEnabled(FeatureKey.WHATSAPP_BOT);
        var cadete = ventaService.cadetePorTelefono(dto != null ? dto.getTelefono() : null);
        Venta tomada = ventaService.tomarSiguienteOId(dto != null ? dto.getVentaId() : null, cadete);
        return ResponseEntity.ok(ventaService.toListado(ventaService.obtenerDetalle(tomada.getId())));
    }

    @PostMapping("/entregar")
    public ResponseEntity<VentaListadoDTO> entregar(@RequestBody BotCadeteAccionDTO dto) {
        tenantFeatureService.requireEnabled(FeatureKey.WHATSAPP_BOT);
        var cadete = ventaService.cadetePorTelefono(dto != null ? dto.getTelefono() : null);
        Long ventaId = exigirVentaId(dto);
        Venta done = ventaService.marcarEntregadoComo(ventaId, cadete);
        return ResponseEntity.ok(ventaService.toListado(ventaService.obtenerDetalle(done.getId())));
    }

    @PostMapping("/cobro")
    public ResponseEntity<VentaListadoDTO> cobro(@RequestBody BotCadeteAccionDTO dto) {
        tenantFeatureService.requireEnabled(FeatureKey.WHATSAPP_BOT);
        var cadete = ventaService.cadetePorTelefono(dto != null ? dto.getTelefono() : null);
        Long ventaId = exigirVentaId(dto);
        VentaCobroDTO cobro = new VentaCobroDTO();
        cobro.setCobrado(dto.getCobrado() == null || Boolean.TRUE.equals(dto.getCobrado()));
        cobro.setMetodoPago(dto.getMetodoPago());
        Venta done = ventaService.actualizarCobroComo(ventaId, cadete, cobro);
        return ResponseEntity.ok(ventaService.toListado(ventaService.obtenerDetalle(done.getId())));
    }

    @PostMapping("/liberar")
    public ResponseEntity<VentaListadoDTO> liberar(@RequestBody BotCadeteAccionDTO dto) {
        tenantFeatureService.requireEnabled(FeatureKey.WHATSAPP_BOT);
        var cadete = ventaService.cadetePorTelefono(dto != null ? dto.getTelefono() : null);
        Long ventaId = exigirVentaId(dto);
        Venta done = ventaService.liberarPedidoComo(ventaId, cadete);
        return ResponseEntity.ok(ventaService.toListado(ventaService.obtenerDetalle(done.getId())));
    }

    private Long exigirVentaId(BotCadeteAccionDTO dto) {
        if (dto == null || dto.getVentaId() == null) {
            throw new BusinessException("Falta el pedido.");
        }
        return dto.getVentaId();
    }

    private String armarObservaciones(ExternalOrderDTO dto) {
        String telefono = primerTexto(dto.getClienteTelefono());
        String extra = primerTexto(dto.getObservaciones());
        StringBuilder sb = new StringBuilder("Pedido WhatsApp");
        if (telefono != null) {
            sb.append(" · Tel: ").append(telefono);
        }
        if (extra != null) {
            sb.append(" · ").append(extra);
        }
        return sb.toString();
    }

    private String primerTexto(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }
}

package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.FeatureKey;
import com.tulumcore.api.entities.Producto;
import com.tulumcore.api.entities.Venta;
import com.tulumcore.api.services.ProductoService;
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

    @Autowired private ProductoService productoService;
    @Autowired private VentaService ventaService;
    @Autowired private TenantFeatureService tenantFeatureService;

    @GetMapping("/buscar")
    public ResponseEntity<List<BotProductoDTO>> buscarParaBot(@RequestParam("q") String query) {
        tenantFeatureService.requireEnabled(FeatureKey.WHATSAPP_BOT);
        List<BotProductoDTO> catalogo = productoService.buscarPorNombre(query).stream()
                .map(this::aDtoPublico)
                .toList();
        return ResponseEntity.ok(catalogo);
    }

    @PostMapping("/pedido")
    public ResponseEntity<?> crearPedidoDesdeBot(@RequestBody ExternalOrderDTO dto) {
        tenantFeatureService.requireEnabled(FeatureKey.WHATSAPP_BOT);

        VentaDTO ventaDto = new VentaDTO();
        ventaDto.setMetodoPago("MERCADO_PAGO");
        ventaDto.setObservaciones("Pedido automático vía WhatsApp: " + dto.getClienteTelefono());

        List<ItemVentaDTO> itemsVenta = new ArrayList<>();
        if (dto.getItems() != null) {
            for (ExternalOrderDTO.ItemBotDTO itemBot : dto.getItems()) {
                ItemVentaDTO iv = new ItemVentaDTO();
                iv.setProductoId(itemBot.getProductoId());
                iv.setCantidad(itemBot.getCantidad());
                itemsVenta.add(iv);
            }
        }
        ventaDto.setItems(itemsVenta);

        try {
            Venta resultado = ventaService.guardar(ventaDto);
            return ResponseEntity.ok("Pedido #" + resultado.getId() + " recibido y en preparación");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al procesar pedido del bot: " + e.getMessage());
        }
    }

    private BotProductoDTO aDtoPublico(Producto producto) {
        return new BotProductoDTO(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getCantidadStock(),
                producto.getMedidas(),
                producto.getImageUrl()
        );
    }
}

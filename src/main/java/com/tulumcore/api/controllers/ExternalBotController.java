package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.FeatureKey;
import com.tulumcore.api.entities.Producto;
import com.tulumcore.api.entities.Venta;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.services.CanalVenta;
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
    public ResponseEntity<PedidoCreadoDTO> crearPedidoDesdeBot(@RequestBody ExternalOrderDTO dto) {
        tenantFeatureService.requireEnabled(FeatureKey.WHATSAPP_BOT);
        if (dto == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException("El pedido necesita al menos un producto.");
        }

        VentaDTO ventaDto = new VentaDTO();
        ventaDto.setCanal(CanalVenta.WHATSAPP);
        ventaDto.setMetodoPago(dto.getMetodoPago());
        ventaDto.setTelefonoContacto(primerTexto(dto.getClienteTelefono()));
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

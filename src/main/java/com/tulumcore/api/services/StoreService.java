package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.controllers.ItemVentaDTO;
import com.tulumcore.api.controllers.PedidoCreadoDTO;
import com.tulumcore.api.controllers.StorePedidoRequest;
import com.tulumcore.api.controllers.StorefrontDTO;
import com.tulumcore.api.controllers.VentaDTO;
import com.tulumcore.api.entities.FeatureKey;
import com.tulumcore.api.entities.Producto;
import com.tulumcore.api.entities.TenantConfig;
import com.tulumcore.api.entities.Venta;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.repositories.TenantConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StoreService {

    @Autowired private TenantConfigRepository tenantConfigRepository;
    @Autowired private TenantFeatureService tenantFeatureService;
    @Autowired private ProductoService productoService;
    @Autowired private VentaService ventaService;

    public StorefrontDTO abrirCatalogo(String tenantSlug) {
        TenantConfig config = abrirTienda(tenantSlug);
        List<Producto> productos = productoService.listarPublicadosEnCatalogo();
        Map<Long, StorefrontDTO.StoreCategoriaDTO> categorias = new LinkedHashMap<>();
        List<StorefrontDTO.StoreProductoDTO> items = new ArrayList<>();
        for (Producto producto : productos) {
            Long categoriaId = producto.getCategoria() != null ? producto.getCategoria().getId() : null;
            String categoriaNombre = producto.getCategoria() != null ? producto.getCategoria().getNombre() : "General";
            if (categoriaId != null) {
                categorias.putIfAbsent(categoriaId, new StorefrontDTO.StoreCategoriaDTO(categoriaId, categoriaNombre));
            }
            items.add(new StorefrontDTO.StoreProductoDTO(
                    producto.getId(),
                    producto.getNombre(),
                    producto.getDescripcion(),
                    producto.getPrecio(),
                    producto.getCantidadStock(),
                    producto.getMedidas(),
                    producto.getImageUrl(),
                    categoriaId,
                    categoriaNombre
            ));
        }
        return new StorefrontDTO(
                config.getTenantId(),
                texto(config.getNombreEmpresa(), "Tienda"),
                config.getLogoUrl(),
                config.getAliasCobro(),
                config.getIvaPorcentaje(),
                config.isPagoEfectivoHabilitado(),
                config.isPagoTransferenciaHabilitado(),
                List.copyOf(categorias.values()),
                items
        );
    }

    @Transactional
    public PedidoCreadoDTO crearPedido(String tenantSlug, StorePedidoRequest request) {
        abrirTienda(tenantSlug);
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("El pedido necesita al menos un producto.");
        }

        String nombre = exigirTexto(request.getNombre(), "Indicá tu nombre.");
        String telefono = exigirTexto(request.getTelefono(), "Indicá un teléfono de contacto.");
        String modalidad = normalizarModalidad(request.getModalidad());
        String direccion = textoOpcional(request.getDireccion());
        if ("ENVIO".equals(modalidad) && direccion == null) {
            throw new BusinessException("El envío necesita una dirección.");
        }

        Map<Long, Producto> publicados = new LinkedHashMap<>();
        for (Producto producto : productoService.listarPublicadosEnCatalogo()) {
            publicados.put(producto.getId(), producto);
        }

        VentaDTO ventaDto = new VentaDTO();
        ventaDto.setCanal(CanalVenta.ECOMMERCE);
        ventaDto.setMetodoPago(request.getMetodoPago());
        ventaDto.setNombreContacto(nombre);
        ventaDto.setTelefonoContacto(telefono);
        ventaDto.setDireccionEntrega("ENVIO".equals(modalidad) ? direccion : null);
        ventaDto.setObservaciones(armarObservaciones(modalidad, request.getObservaciones()));
        ventaDto.setCobrado(false);

        List<ItemVentaDTO> items = new ArrayList<>();
        for (StorePedidoRequest.Item linea : request.getItems()) {
            if (linea == null || linea.getProductoId() == null) {
                throw new BusinessException("Hay un producto inválido en el carrito.");
            }
            Producto publicado = publicados.get(linea.getProductoId());
            if (publicado == null) {
                throw new BusinessException("Un producto del carrito ya no está en la tienda.");
            }
            ItemVentaDTO item = new ItemVentaDTO();
            item.setProductoId(linea.getProductoId());
            item.setCantidad(linea.getCantidad());
            items.add(item);
        }
        ventaDto.setItems(items);

        Venta venta = ventaService.guardar(ventaDto);
        return PedidoCreadoDTO.desde(venta);
    }

    private TenantConfig abrirTienda(String tenantSlug) {
        String tenant = tenantSlug == null ? "" : tenantSlug.trim().toLowerCase();
        if (!tenant.matches("[a-z0-9][a-z0-9_-]{2,40}")) {
            throw new ResourceNotFoundException("Tienda no encontrada.");
        }
        TenantContext.setCurrentTenant(tenant);
        TenantConfig config = tenantConfigRepository.findByTenantId(tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada."));
        if (!config.isActivo()) {
            throw new BusinessException("Esta tienda no está disponible.");
        }
        tenantFeatureService.requireEnabled(FeatureKey.CUSTOMER_CATALOG);
        return config;
    }

    private String normalizarModalidad(String raw) {
        String valor = raw == null ? "" : raw.trim().toUpperCase();
        if ("RETIRO".equals(valor) || "TAKEAWAY".equals(valor)) {
            return "RETIRO";
        }
        if ("ENVIO".equals(valor) || "DELIVERY".equals(valor)) {
            return "ENVIO";
        }
        throw new BusinessException("Elegí envío o retiro.");
    }

    private String armarObservaciones(String modalidad, String extra) {
        StringBuilder sb = new StringBuilder("Pedido tienda online");
        sb.append(" · ").append("ENVIO".equals(modalidad) ? "Envío" : "Retiro");
        String nota = textoOpcional(extra);
        if (nota != null) {
            sb.append(" · ").append(nota);
        }
        return sb.toString();
    }

    private String exigirTexto(String valor, String error) {
        String texto = textoOpcional(valor);
        if (texto == null) {
            throw new BusinessException(error);
        }
        return texto;
    }

    private String textoOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    private String texto(String valor, String fallback) {
        String texto = textoOpcional(valor);
        return texto != null ? texto : fallback;
    }
}

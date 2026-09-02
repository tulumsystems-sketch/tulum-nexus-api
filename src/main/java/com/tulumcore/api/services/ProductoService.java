package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.Categoria;
import com.tulumcore.api.entities.FeatureKey;
import com.tulumcore.api.entities.MovementType;
import com.tulumcore.api.entities.Producto;
import com.tulumcore.api.entities.ProductoTipo;
import com.tulumcore.api.entities.TenantConfig;
import com.tulumcore.api.entities.Usuario;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.repositories.CategoriaRepository;
import com.tulumcore.api.repositories.ProductoRecetaRepository;
import com.tulumcore.api.repositories.ProductoRepository;
import com.tulumcore.api.repositories.TenantConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    private static final Logger log = LoggerFactory.getLogger(ProductoService.class);

    private static final String TENANT_FOGON = "fogon";

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ProductoRecetaRepository recetaRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private StockMovementService stockMovementService;

    @Autowired
    private AuditoryLogService auditoryLogService;

    @Autowired
    private TenantFeatureService tenantFeatureService;

    @Autowired
    private TenantConfigRepository tenantConfigRepository;

    /**
     * Margen configurado para el tenant vigente. null = el tenant carga el precio
     * de venta a mano y no queremos derivarlo del costo.
     */
    public Double getMargenPorDefecto() {
        String tenant = TenantContext.getCurrentTenant();
        return tenantConfigRepository.findByTenantId(tenant)
                .map(TenantConfig::getMargenPorDefecto)
                .orElse(null);
    }

    /**
     * Precio de venta derivado del costo. Devuelve null si falta el costo o si no hay
     * margen aplicable, para que el llamador respete el precio cargado a mano.
     */
    public Double calcularPrecioVenta(Double precioCosto, Double margenProducto) {
        if (precioCosto == null) {
            return null;
        }
        Double margen = margenProducto != null ? margenProducto : getMargenPorDefecto();
        if (margen == null) {
            return null;
        }
        return precioCosto * (1 + margen / 100.0);
    }

    @Transactional
    public List<Producto> getAllProductos() {
        sembrarCartaParrillaSiVacia();
        String tenant = TenantContext.getCurrentTenant();
        return productoRepository.findAllByTenantId(tenant);
    }

    public Optional<Producto> getProductoById(Long id) {
        String tenant = TenantContext.getCurrentTenant();
        return productoRepository.findByIdAndTenantId(id, tenant);
    }

    @Transactional
    public List<Producto> buscarPorNombre(String query) {
        sembrarCartaParrillaSiVacia();
        String tenant = TenantContext.getCurrentTenant();
        String normalizedQuery = query != null ? query.trim() : "";
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }
        return productoRepository.buscarCatalogo(tenant, normalizedQuery, PageRequest.of(0, 40));
    }

    /**
     * Fogón demo: si el tenant todavía no cargó carta, deja una parrilla mínima
     * para que el bot de WhatsApp no responda "carta vacía".
     */
    void sembrarCartaParrillaSiVacia() {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null || !TENANT_FOGON.equalsIgnoreCase(tenant)) {
            return;
        }
        if (!productoRepository.findAllByTenantId(tenant).isEmpty()) {
            return;
        }

        Categoria parrilla = categoriaDemo(tenant, "Parrilla");
        Categoria entradas = categoriaDemo(tenant, "Entradas");
        Categoria guarniciones = categoriaDemo(tenant, "Guarniciones");
        Categoria bebidas = categoriaDemo(tenant, "Bebidas");
        Categoria postres = categoriaDemo(tenant, "Postres");

        Object[][] platos = {
                {parrilla, "Asado", "Tira de asado a la parrilla", "8500", "porción"},
                {parrilla, "Vacío", "Vacío a las brasas", "9200", "porción"},
                {parrilla, "Entraña", "Entraña jugosa", "9800", "porción"},
                {parrilla, "Milanesa", "Milanesa de ternera", "6500", "unidad"},
                {entradas, "Empanadas", "Empanadas de carne", "1200", "unidad"},
                {entradas, "Provoleta", "Provoleta a la parrilla", "4200", "unidad"},
                {guarniciones, "Papas fritas", "Papas fritas caseras", "2800", "porción"},
                {guarniciones, "Ensalada mixta", "Lechuga, tomate y cebolla", "3200", "porción"},
                {bebidas, "Gaseosa", "Línea 500 ml", "1800", "unidad"},
                {bebidas, "Agua", "Agua mineral 500 ml", "1200", "unidad"},
                {postres, "Flan", "Flan casero", "2500", "unidad"}
        };
        for (Object[] row : platos) {
            Producto producto = new Producto();
            producto.setCategoria((Categoria) row[0]);
            producto.setNombre((String) row[1]);
            producto.setDescripcion((String) row[2]);
            producto.setPrecio(Double.parseDouble((String) row[3]));
            producto.setMedidas((String) row[4]);
            producto.setCantidadStock(80d);
            producto.setStockMinimo(5);
            producto.setTenantId(tenant);
            productoRepository.save(producto);
        }
        productoRepository.flush();
        log.info("Carta parrilla sembrada para tenant {} ({} platos)", tenant, platos.length);
    }

    private Categoria categoriaDemo(String tenant, String nombre) {
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setUnidadMedida("UNIDAD");
        categoria.setTenantId(tenant);
        return categoriaRepository.saveAndFlush(categoria);
    }

    public Optional<Producto> buscarPorCodigoBarras(String codigoBarras) {
        tenantFeatureService.requireEnabled(FeatureKey.POS_BARCODE);
        String tenant = TenantContext.getCurrentTenant();
        String normalizedCodigo = normalizarCodigoBarras(codigoBarras);
        if (normalizedCodigo == null) {
            return Optional.empty();
        }
        return productoRepository.findByCodigoBarrasAndTenantId(normalizedCodigo, tenant);
    }

    public Producto createOrUpdateProducto(Producto producto) {
        boolean isNew = producto.getId() == null;
        String tenant = TenantContext.getCurrentTenant();
        producto.setTenantId(tenant);
        producto.setCodigoBarras(normalizarCodigoBarras(producto.getCodigoBarras()));
        validarCodigoBarrasUnico(producto, tenant);

        // Si sólo llegó el costo, derivamos el precio de venta con el margen del tenant.
        if (producto.getPrecio() == null) {
            if (!producto.isVendible()) {
                producto.setPrecio(0.0);
            } else {
                Double precioDerivado = calcularPrecioVenta(producto.getPrecioCosto(), producto.getMargenPorcentaje());
                if (precioDerivado == null) {
                    throw new BusinessException("Cargá el precio de venta o el precio de costo con un margen configurado.");
                }
                producto.setPrecio(precioDerivado);
            }
        }
        if (producto.getTipo() == null || producto.getTipo().isBlank()) {
            producto.setTipo(ProductoTipo.ELABORADO);
        }

        String detalleAnterior = null;
        if (!isNew) {
            detalleAnterior = productoRepository.findByIdAndTenantId(producto.getId(), tenant)
                    .map(this::detalleProducto)
                    .orElse(null);
        }

        Producto saved = productoRepository.save(producto);
        auditoryLogService.registrar(isNew ? "CREATE" : "UPDATE", "PRODUCTO", saved.getId(),
                (isNew ? "Producto creado: " : "Producto actualizado: ") + saved.getNombre(),
                detalleAnterior, detalleProducto(saved));
        return saved;
    }

    public void deleteProducto(Long id) {
        getProductoById(id).ifPresent(p -> {
            String tenant = TenantContext.getCurrentTenant();
            if (recetaRepository.existsByInsumoIdAndTenantId(id, tenant)) {
                throw new BusinessException("No se puede borrar: está usado como ingrediente en un plato de la carta.");
            }
            String detalleAnterior = detalleProducto(p);
            productoRepository.delete(p);
            auditoryLogService.registrar("DELETE", "PRODUCTO", id,
                    "Producto eliminado: " + p.getNombre(), detalleAnterior, null);
        });
    }

    @Transactional
    public void adjustStock(Long id, int cantidad) {
        Producto producto = getProductoById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        Usuario usuario = stockMovementService.getCurrentUser();
        stockMovementService.registrar(MovementType.AJUSTE, producto, usuario,
                cantidad, "Ajuste manual de stock", null, null);
    }

    public List<Producto> getLatestProductos(int limit) {
        String tenant = TenantContext.getCurrentTenant();
        return productoRepository.findAllByTenantId(tenant)
                .stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .limit(limit)
                .toList();
    }

    private String detalleProducto(Producto producto) {
        return auditoryLogService.detalle(
                "nombre", producto.getNombre(),
                "precio", producto.getPrecio(),
                "precioCosto", producto.getPrecioCosto(),
                "margenPorcentaje", producto.getMargenPorcentaje(),
                "stock", producto.getCantidadStock(),
                "stockMinimo", producto.getStockMinimo(),
                "tipo", producto.getTipo(),
                "vendible", producto.isVendible(),
                "medidas", producto.getMedidas(),
                "codigoBarras", producto.getCodigoBarras(),
                "categoriaId", producto.getCategoria() != null ? producto.getCategoria().getId() : null
        );
    }

    public List<Producto> listarPublicadosEnCatalogo() {
        if (!catalogoPublicoHabilitado()) {
            return List.of();
        }
        String tenant = TenantContext.getCurrentTenant();
        return productoRepository.findAllByTenantId(tenant).stream()
                .filter(Producto::isPublicadoEnCatalogo)
                .filter(Producto::isVendible)
                .toList();
    }

    public boolean catalogoPublicoHabilitado() {
        return tenantFeatureService.isEnabled(FeatureKey.CUSTOMER_CATALOG);
    }

    private String normalizarCodigoBarras(String codigoBarras) {
        if (codigoBarras == null || codigoBarras.trim().isEmpty()) {
            return null;
        }
        return codigoBarras.trim();
    }

    private void validarCodigoBarrasUnico(Producto producto, String tenant) {
        String codigoBarras = producto.getCodigoBarras();
        if (codigoBarras == null) {
            return;
        }

        productoRepository.findByCodigoBarrasAndTenantId(codigoBarras, tenant)
                .filter(existing -> producto.getId() == null || !existing.getId().equals(producto.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException("Ya existe un producto con este codigo de barras.");
                });
    }
}

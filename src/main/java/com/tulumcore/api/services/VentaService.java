package com.tulumcore.api.services;

import com.tulumcore.api.controllers.ItemVentaDTO;
import com.tulumcore.api.controllers.VentaDTO;
import com.tulumcore.api.entities.*;
import com.tulumcore.api.repositories.*;
import com.tulumcore.api.config.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class VentaService {

    @Autowired private VentaRepository ventaRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private CajaRepository cajaRepository; // Inyectamos la caja

    @Transactional
    public Venta guardar(VentaDTO dto) {
        String tenant = TenantContext.getCurrentTenant();

        // 1. Validar que la CAJA esté ABIERTA antes de empezar
        Caja caja = cajaRepository.findByEstadoAndTenantId("ABIERTA", tenant)
                .orElseThrow(() -> new RuntimeException("OPERACIÓN DENEGADA: Debe abrir caja para realizar ventas."));

        Venta venta = new Venta();
        venta.setObservaciones(dto.observaciones);
        venta.setMoneda("ARS");

        String metodo = (dto.metodoPago != null) ? dto.metodoPago : "MERCADO_PAGO";
        venta.setMetodoPago(metodo);

        if (dto.clienteId != null && dto.clienteId > 0) {
            Cliente cliente = clienteRepository.findById(dto.clienteId).orElse(null);
            venta.setCliente(cliente);
        }

        List<ItemVenta> items = new ArrayList<>();
        double subtotal = 0;

        for (ItemVentaDTO itemDto : dto.items) {
            Producto p = productoRepository.findById(itemDto.productoId)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + itemDto.productoId));

            // 2. Control de Stock
            if (p.getCantidadStock() < itemDto.cantidad) {
                throw new RuntimeException("Stock insuficiente para: " + p.getNombre());
            }

            // 3. Descontar Stock
            p.setCantidadStock(p.getCantidadStock() - itemDto.cantidad);
            productoRepository.save(p);

            ItemVenta item = new ItemVenta();
            item.setVenta(venta);
            item.setProducto(p);
            item.setCantidad(itemDto.cantidad);
            item.setPrecioUnitario(p.getPrecio());
            items.add(item);
            subtotal += p.getPrecio() * itemDto.cantidad;
        }

        venta.setItems(items);
        venta.setTotalNeto(subtotal);
        venta.setTotalIva(subtotal * 0.21);
        double totalFinal = subtotal * 1.21;
        venta.setTotalFinal(totalFinal);

        // 4. Lógica de Cobro y Actualización de Caja
        if ("EFECTIVO".equalsIgnoreCase(metodo)) {
            venta.setEstado("PAGADA");
            double abonado = (dto.montoAbonado != null) ? dto.montoAbonado : totalFinal;
            venta.setMontoAbonado(abonado);
            venta.setVuelto(Math.max(0, abonado - totalFinal));

            // Actualizamos montos de Efectivo en la Caja
            double acumuladoEfectivo = (caja.getMontoVentasEfectivo() != null ? caja.getMontoVentasEfectivo() : 0.0) + totalFinal;
            caja.setMontoVentasEfectivo(acumuladoEfectivo);

            // Lo esperado es: Inicial + Todo lo vendido en efectivo
            caja.setMontoFinalEsperado(caja.getMontoInicial() + acumuladoEfectivo);

        } else if ("MERCADO_PAGO".equalsIgnoreCase(metodo)) {
            venta.setEstado("PAGADA"); // Podés dejarlo en PENDIENTE si integrás Webhook, por ahora PAGADA
            double acumuladoMP = (caja.getMontoVentasMP() != null ? caja.getMontoVentasMP() : 0.0) + totalFinal;
            caja.setMontoVentasMP(acumuladoMP);
        }

        // Guardamos los cambios en la caja
        cajaRepository.save(caja);

        return ventaRepository.save(venta);
    }

    @Transactional
    public Venta anularVenta(Long idVenta) {
        String tenant = TenantContext.getCurrentTenant();
        Venta venta = ventaRepository.findById(idVenta)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        if ("ANULADA".equals(venta.getEstado())) return venta;

        // 5. Devolver Stock al anular
        for (ItemVenta item : venta.getItems()) {
            Producto p = item.getProducto();
            p.setCantidadStock(p.getCantidadStock() + item.getCantidad());
            productoRepository.save(p);
        }

        // 6. Restar de la Caja si la venta era del turno actual (Opcional, según política)
        // Por ahora solo anulamos la venta y devolvemos stock.

        venta.setEstado("ANULADA");
        return ventaRepository.save(venta);
    }

    public void marcarComoPagada(Long idVenta) {
        Venta venta = ventaRepository.findById(idVenta).orElseThrow();
        venta.setEstado("PAGADA");
        ventaRepository.save(venta);
    }
}
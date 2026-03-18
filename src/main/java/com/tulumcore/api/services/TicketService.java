package com.tulumcore.api.services;

import com.tulumcore.api.entities.Venta;
import com.tulumcore.api.entities.ItemVenta;
import com.tulumcore.api.entities.TenantConfig;
import com.tulumcore.api.repositories.TenantConfigRepository; // Asegurate de tener este repo
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;

@Service
public class TicketService {

    @Autowired
    private TenantConfigRepository tenantConfigRepository;

    public byte[] generarTicketPDF(Venta venta) throws Exception {
        // 1. Buscamos el nombre de la empresa dinámicamente
        String nombreEmpresa = tenantConfigRepository.findByTenantId(venta.getTenantId())
                .map(TenantConfig::getNombreEmpresa)
                .filter(name -> !name.isBlank())
                .orElse("TULUM SYSTEMS"); // Fallback por si no hay config

        Document document = new Document(new Rectangle(226, 800));
        document.setMargins(10, 10, 10, 10);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        // Estilos
        Font bold = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        Font normal = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);

        // Encabezado DINÁMICO
        Paragraph titulo = new Paragraph(nombreEmpresa.toUpperCase(), bold);
        titulo.setAlignment(Element.ALIGN_CENTER);
        document.add(titulo);

        document.add(new Paragraph("Comprobante de Venta #" + venta.getId(), normal));
        document.add(new Paragraph("Fecha: " + venta.getFecha(), normal));
        document.add(new Paragraph("----------------------------------", normal));

        // Items
        for (ItemVenta item : venta.getItems()) {
            document.add(new Paragraph(item.getProducto().getNombre(), normal));
            document.add(new Paragraph(item.getCantidad() + " x $" + item.getPrecioUnitario() + " = $" + (item.getCantidad() * item.getPrecioUnitario()), normal));
        }

        document.add(new Paragraph("----------------------------------", normal));
        document.add(new Paragraph("TOTAL: $" + venta.getTotalFinal(), bold));
        document.add(new Paragraph("Pago: " + venta.getMetodoPago(), normal));

        document.add(new Paragraph("\n¡Gracias por su compra!", normal));

        document.close();
        return out.toByteArray();
    }
}
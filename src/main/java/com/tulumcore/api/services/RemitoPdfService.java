package com.tulumcore.api.services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.tulumcore.api.entities.ItemRemito;
import com.tulumcore.api.entities.PagoRemito;
import com.tulumcore.api.entities.Remito;
import com.tulumcore.api.entities.TenantConfig;
import com.tulumcore.api.repositories.PagoRemitoRepository;
import com.tulumcore.api.repositories.TenantConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Genera el remito en PDF en hoja A4, siguiendo el mismo enfoque con iText que TicketService.
 */
@Service
public class RemitoPdfService {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Font TITULO = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
    private static final Font SUBTITULO = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.DARK_GRAY);
    private static final Font ETIQUETA = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.GRAY);
    private static final Font TEXTO = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font TEXTO_BOLD = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    private static final Font CABECERA_TABLA = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);
    private static final Font TOTAL = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD);
    private static final Font PIE = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY);

    @Autowired
    private TenantConfigRepository tenantConfigRepository;

    @Autowired
    private PagoRemitoRepository pagoRemitoRepository;

    public byte[] generarRemitoPDF(Remito remito) throws Exception {
        TenantConfig config = tenantConfigRepository.findByTenantId(remito.getTenantId()).orElse(null);
        String nombreEmpresa = config != null && config.getNombreEmpresa() != null && !config.getNombreEmpresa().isBlank()
                ? config.getNombreEmpresa()
                : "TULUM SYSTEMS";
        String logoUrl = config != null ? config.getLogoUrl() : null;
        String aliasCobro = config != null ? config.getAliasCobro() : null;

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        document.add(encabezado(nombreEmpresa, logoUrl, remito));
        document.add(espacio(12));
        document.add(datosEntrega(remito));
        document.add(espacio(12));
        document.add(tablaItems(remito));
        document.add(espacio(8));
        document.add(totales(remito));
        document.add(espacio(12));
        document.add(estadoDeCobranza(remito, aliasCobro));

        List<PagoRemito> pagos = List.of();
        try {
            pagos = pagoRemitoRepository
                    .findAllByTenantIdAndRemitoIdOrderByFechaDesc(remito.getTenantId(), remito.getId());
        } catch (Exception e) {
            // Si la tabla de pagos todavía no existe, el remito se emite igual sin el historial.
        }
        if (!pagos.isEmpty()) {
            document.add(espacio(10));
            document.add(tablaPagos(pagos));
        }

        document.add(espacio(18));
        Paragraph pie = new Paragraph(
                "Documento no valido como factura. Emitido por " + nombreEmpresa + " - Tulum Core.", PIE);
        pie.setAlignment(Element.ALIGN_CENTER);
        document.add(pie);

        document.close();
        return out.toByteArray();
    }

    private PdfPTable encabezado(String nombreEmpresa, String logoUrl, Remito remito) throws DocumentException {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{60, 40});

        PdfPCell empresa = new PdfPCell();
        empresa.setBorder(Rectangle.NO_BORDER);
        Image logo = cargarLogo(logoUrl);
        if (logo != null) {
            logo.scaleToFit(120, 60);
            empresa.addElement(logo);
        }
        empresa.addElement(new Paragraph(nombreEmpresa.toUpperCase(Locale.ROOT), TITULO));
        empresa.addElement(new Paragraph("Remito de entrega", SUBTITULO));
        tabla.addCell(empresa);

        PdfPCell datos = new PdfPCell();
        datos.setBorder(Rectangle.BOX);
        datos.setBorderColor(BaseColor.LIGHT_GRAY);
        datos.setPadding(10);
        datos.addElement(parrafo("REMITO NRO", ETIQUETA));
        datos.addElement(parrafo(remito.getNroRemito() != null ? remito.getNroRemito() : String.valueOf(remito.getId()), TEXTO_BOLD));
        datos.addElement(parrafo("FECHA", ETIQUETA));
        datos.addElement(parrafo(remito.getFecha() != null ? remito.getFecha().format(FECHA_HORA) : "-", TEXTO));
        datos.addElement(parrafo("ESTADO DE ENTREGA", ETIQUETA));
        datos.addElement(parrafo(remito.getEstado() != null ? remito.getEstado().replace('_', ' ') : "-", TEXTO));
        tabla.addCell(datos);

        return tabla;
    }

    private PdfPTable datosEntrega(Remito remito) throws DocumentException {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{50, 50});

        tabla.addCell(bloque("DESTINATARIO", valorOGuion(remito.getNombreDestinatario())));
        tabla.addCell(bloque("TELEFONO", valorOGuion(remito.getTelefonoDestinatario())));
        tabla.addCell(bloque("DIRECCION DE ENTREGA", valorOGuion(remito.getDireccionEntrega())));

        String cliente = "-";
        if (remito.getCliente() != null) {
            cliente = (nvl(remito.getCliente().getNombre()) + " " + nvl(remito.getCliente().getApellido())).trim();
            if (cliente.isEmpty()) {
                cliente = "-";
            }
        }
        tabla.addCell(bloque("CLIENTE", cliente));

        PdfPCell observaciones = bloque("OBSERVACIONES", valorOGuion(remito.getObservaciones()));
        observaciones.setColspan(2);
        tabla.addCell(observaciones);

        return tabla;
    }

    private PdfPTable tablaItems(Remito remito) throws DocumentException {
        PdfPTable tabla = new PdfPTable(4);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{52, 12, 18, 18});

        tabla.addCell(celdaCabecera("DESCRIPCION", Element.ALIGN_LEFT));
        tabla.addCell(celdaCabecera("CANT.", Element.ALIGN_CENTER));
        tabla.addCell(celdaCabecera("PRECIO UNIT.", Element.ALIGN_RIGHT));
        tabla.addCell(celdaCabecera("TOTAL", Element.ALIGN_RIGHT));

        List<ItemRemito> items = remito.getItems();
        if (items == null || items.isEmpty()) {
            PdfPCell vacio = new PdfPCell(new Phrase("El remito no tiene articulos cargados.", TEXTO));
            vacio.setColspan(4);
            vacio.setPadding(8);
            tabla.addCell(vacio);
            return tabla;
        }

        for (ItemRemito item : items) {
            tabla.addCell(celda(descripcionItem(item), Element.ALIGN_LEFT));
            tabla.addCell(celda(String.valueOf(item.getCantidad() != null ? item.getCantidad() : 0), Element.ALIGN_CENTER));
            tabla.addCell(celda(moneda(item.getPrecioUnitario()), Element.ALIGN_RIGHT));
            tabla.addCell(celda(moneda(item.getTotalLinea()), Element.ALIGN_RIGHT));
        }

        return tabla;
    }

    private PdfPTable totales(Remito remito) throws DocumentException {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(45);
        tabla.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.setWidths(new float[]{50, 50});

        PdfPCell etiqueta = new PdfPCell(new Phrase("TOTAL DEL REMITO", TOTAL));
        etiqueta.setBorder(Rectangle.TOP);
        etiqueta.setBorderColor(BaseColor.DARK_GRAY);
        etiqueta.setPadding(8);
        tabla.addCell(etiqueta);

        PdfPCell valor = new PdfPCell(new Phrase(moneda(remito.getTotal()), TOTAL));
        valor.setBorder(Rectangle.TOP);
        valor.setBorderColor(BaseColor.DARK_GRAY);
        valor.setPadding(8);
        valor.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(valor);

        return tabla;
    }

    private PdfPTable estadoDeCobranza(Remito remito, String aliasCobro) throws DocumentException {
        PdfPTable tabla = new PdfPTable(3);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{34, 33, 33});

        tabla.addCell(bloque("ESTADO DE PAGO", etiquetaEstadoPago(remito.getEstadoPago())));
        tabla.addCell(bloque("COBRADO", moneda(remito.getMontoPagado())));
        tabla.addCell(bloque("SALDO PENDIENTE", moneda(remito.getSaldoPendiente())));

        if (aliasCobro != null && !aliasCobro.isBlank()) {
            PdfPCell alias = bloque("ALIAS PARA TRANSFERENCIAS", aliasCobro);
            alias.setColspan(3);
            tabla.addCell(alias);
        }

        return tabla;
    }

    private PdfPTable tablaPagos(List<PagoRemito> pagos) throws DocumentException {
        PdfPTable tabla = new PdfPTable(4);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{22, 22, 20, 36});

        tabla.addCell(celdaCabecera("PAGOS: FECHA", Element.ALIGN_LEFT));
        tabla.addCell(celdaCabecera("MEDIO", Element.ALIGN_LEFT));
        tabla.addCell(celdaCabecera("MONTO", Element.ALIGN_RIGHT));
        tabla.addCell(celdaCabecera("OBSERVACIONES", Element.ALIGN_LEFT));

        for (PagoRemito pago : pagos) {
            tabla.addCell(celda(pago.getFecha() != null ? pago.getFecha().format(FECHA_HORA) : "-", Element.ALIGN_LEFT));
            tabla.addCell(celda(pago.getMetodoPago() != null ? pago.getMetodoPago().replace('_', ' ') : "-", Element.ALIGN_LEFT));
            tabla.addCell(celda(moneda(pago.getMonto()), Element.ALIGN_RIGHT));
            tabla.addCell(celda(valorOGuion(pago.getObservaciones()), Element.ALIGN_LEFT));
        }

        return tabla;
    }

    private Image cargarLogo(String logoUrl) {
        if (logoUrl == null || logoUrl.isBlank()) {
            return null;
        }
        try {
            return Image.getInstance(URI.create(logoUrl.trim()).toURL());
        } catch (Exception e) {
            // Si el logo no se puede descargar el remito se emite igual, solo con el nombre del negocio.
            return null;
        }
    }

    private String descripcionItem(ItemRemito item) {
        String nombre = item.getProducto() != null ? nvl(item.getProducto().getNombre()) : "";
        String descripcion = nvl(item.getDescripcion());
        if (!nombre.isEmpty() && !descripcion.isEmpty() && !nombre.equalsIgnoreCase(descripcion)) {
            return nombre + " - " + descripcion;
        }
        if (!nombre.isEmpty()) {
            return nombre;
        }
        return descripcion.isEmpty() ? "Articulo sin descripcion" : descripcion;
    }

    private String etiquetaEstadoPago(String estadoPago) {
        if (estadoPago == null) {
            return "IMPAGO";
        }
        switch (estadoPago) {
            case "PAGADO":
                return "PAGADO";
            case "PAGADO_PARCIAL":
                return "PAGADO PARCIAL";
            default:
                return "IMPAGO";
        }
    }

    private PdfPCell bloque(String etiqueta, String valor) {
        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.BOX);
        celda.setBorderColor(BaseColor.LIGHT_GRAY);
        celda.setPadding(8);
        celda.addElement(parrafo(etiqueta, ETIQUETA));
        celda.addElement(parrafo(valor, TEXTO_BOLD));
        return celda;
    }

    private PdfPCell celdaCabecera(String texto, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, CABECERA_TABLA));
        celda.setBackgroundColor(BaseColor.DARK_GRAY);
        celda.setHorizontalAlignment(alineacion);
        celda.setPadding(6);
        return celda;
    }

    private PdfPCell celda(String texto, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, TEXTO));
        celda.setHorizontalAlignment(alineacion);
        celda.setPadding(6);
        celda.setBorderColor(BaseColor.LIGHT_GRAY);
        return celda;
    }

    private Paragraph parrafo(String texto, Font font) {
        Paragraph p = new Paragraph(texto, font);
        p.setLeading(12f);
        return p;
    }

    private Paragraph espacio(float alto) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(alto);
        return p;
    }

    private String moneda(Double valor) {
        return String.format(Locale.US, "$ %,.2f", valor != null ? valor : 0.0);
    }

    private String valorOGuion(String valor) {
        return valor == null || valor.isBlank() ? "-" : valor;
    }

    private String nvl(String valor) {
        return valor == null ? "" : valor;
    }
}

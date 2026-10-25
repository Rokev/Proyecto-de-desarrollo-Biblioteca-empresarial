package bussinesLayer.util;

import bussinesLayer.dto.ReporteDTO;
import bussinesLayer.exception.BusinessValidationException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Exporta un reporte a PDF ( 78) con OpenPDF: A4 horizontal, encabezado, resumen y tabla. */
@Component
public class ReportePdfExportador {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Color COLOR_ENCABEZADO = new Color(0x2F, 0x3E, 0x56);
    private static final Color COLOR_FILA_PAR = new Color(0xF3, 0xF5, 0xF8);

    public byte[] exportar(ReporteDTO reporte) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4.rotate(), 28, 28, 32, 32);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            Font negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);

            documento.add(new Paragraph(reporte.getTitulo(), titulo));
            documento.add(new Paragraph("Generado: " + reporte.getGeneradoEn().format(FECHA_HORA)
                    + "   |   Desde: " + (reporte.getDesde() != null ? reporte.getDesde() : "-")
                    + "   |   Hasta: " + (reporte.getHasta() != null ? reporte.getHasta() : "-")
                    + "   |   Sede: " + (reporte.getSedeId() != null ? reporte.getSedeId() : "Todas"), normal));
            documento.add(new Paragraph(" "));

            if (!reporte.getResumen().isEmpty()) {
                documento.add(new Paragraph("Resumen", negrita));
                PdfPTable resumen = new PdfPTable(2);
                resumen.setWidthPercentage(45);
                resumen.setHorizontalAlignment(Element.ALIGN_LEFT);
                resumen.setSpacingBefore(4);
                resumen.setSpacingAfter(10);
                for (Map.Entry<String, String> e : reporte.getResumen().entrySet()) {
                    resumen.addCell(celda(e.getKey(), normal, null));
                    resumen.addCell(celda(e.getValue(), negrita, null));
                }
                documento.add(resumen);
            }

            documento.add(new Paragraph("Detalle (" + reporte.getTotalRegistros() + " registros)", negrita));
            if (reporte.getFilas().isEmpty()) {
                documento.add(new Paragraph("No hay datos para los filtros seleccionados.", normal));
            } else {
                documento.add(tabla(reporte));
            }
        } catch (DocumentException e) {
            throw new BusinessValidationException("No se pudo generar el PDF del reporte: " + e.getMessage());
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return salida.toByteArray();
    }

    private PdfPTable tabla(ReporteDTO reporte) {
        List<String> columnas = reporte.getColumnas();
        PdfPTable tabla = new PdfPTable(columnas.size());
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(6);
        tabla.setHeaderRows(1); // el encabezado se repite en cada página

        Font fuenteEncabezado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        Font fuenteCelda = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);

        for (String columna : columnas) {
            tabla.addCell(celda(columna, fuenteEncabezado, COLOR_ENCABEZADO));
        }
        int n = 0;
        for (List<String> fila : reporte.getFilas()) {
            Color fondo = n++ % 2 == 1 ? COLOR_FILA_PAR : null;
            for (int i = 0; i < columnas.size(); i++) {
                tabla.addCell(celda(i < fila.size() ? fila.get(i) : "", fuenteCelda, fondo));
            }
        }
        return tabla;
    }

    private PdfPCell celda(String texto, Font fuente, Color fondo) {
        PdfPCell celda = new PdfPCell(new Phrase(texto == null ? "" : texto, fuente));
        celda.setPadding(4);
        celda.setBorderColor(new Color(0xD5, 0xD9, 0xE0));
        if (fondo != null) {
            celda.setBackgroundColor(fondo);
        }
        return celda;
    }
}

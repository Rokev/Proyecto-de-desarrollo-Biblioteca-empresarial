package bussinesLayer.util;

import bussinesLayer.dto.ReporteDTO;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Exporta un reporte a CSV (RF-78). UTF-8 con BOM para que Excel respete las tildes,
 * campos entre comillas cuando hace falta y protección contra inyección de fórmulas.
 */
@Component
public class ReporteCsvExportador {

    private static final String SEPARADOR = ",";
    private static final String SALTO = "\r\n";
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] exportar(ReporteDTO reporte) {
        StringBuilder sb = new StringBuilder();
        sb.append('﻿');

        sb.append(linea(List.of(reporte.getTitulo()))).append(SALTO);
        sb.append(linea(List.of("Generado", reporte.getGeneradoEn().format(FECHA_HORA)))).append(SALTO);
        sb.append(linea(List.of("Desde", reporte.getDesde() != null ? reporte.getDesde().toString() : "-"))).append(SALTO);
        sb.append(linea(List.of("Hasta", reporte.getHasta() != null ? reporte.getHasta().toString() : "-"))).append(SALTO);
        sb.append(linea(List.of("Sede", reporte.getSedeId() != null ? reporte.getSedeId().toString() : "Todas"))).append(SALTO);

        if (!reporte.getResumen().isEmpty()) {
            sb.append(SALTO).append(linea(List.of("RESUMEN"))).append(SALTO);
            for (Map.Entry<String, String> e : reporte.getResumen().entrySet()) {
                sb.append(linea(List.of(e.getKey(), e.getValue()))).append(SALTO);
            }
        }

        sb.append(SALTO).append(linea(reporte.getColumnas())).append(SALTO);
        for (List<String> fila : reporte.getFilas()) {
            sb.append(linea(fila)).append(SALTO);
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String linea(List<String> valores) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valores.size(); i++) {
            if (i > 0) {
                sb.append(SEPARADOR);
            }
            sb.append(escapar(valores.get(i)));
        }
        return sb.toString();
    }

    public String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        // Una celda que empieza con = + - @ se interpretaría como fórmula en Excel/Sheets.
        if (!valor.isEmpty() && "=+-@\t\r".indexOf(valor.charAt(0)) >= 0 && !esNumero(valor)) {
            valor = "'" + valor;
        }
        if (valor.contains(SEPARADOR) || valor.contains("\"") || valor.contains("\n") || valor.contains("\r")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    private boolean esNumero(String valor) {
        try {
            Double.parseDouble(valor);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

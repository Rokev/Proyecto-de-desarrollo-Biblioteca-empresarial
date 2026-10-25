package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Reporte generado: encabezados, filas y resumen listos para mostrar o exportar")
public class ReporteDTO {

    @Schema(description = "Tipo de reporte", example = "PRESTAMOS")
    private String tipo;

    @Schema(description = "Título del reporte", example = "Reporte de préstamos")
    private String titulo;

    @Schema(description = "Momento en que se generó", example = "2026-10-25T14:30:00")
    private LocalDateTime generadoEn;

    @Schema(description = "Fecha inicial del filtro (inclusive)", example = "2026-10-01")
    private LocalDate desde;

    @Schema(description = "Fecha final del filtro (inclusive)", example = "2026-10-31")
    private LocalDate hasta;

    @Schema(description = "Sede usada como filtro (nulo = todas)", example = "1")
    private Long sedeId;

    @Schema(description = "Nombres de las columnas de la tabla")
    private List<String> columnas = new ArrayList<>();

    @Schema(description = "Filas de la tabla; cada fila tiene un valor por columna")
    private List<List<String>> filas = new ArrayList<>();

    @Schema(description = "Indicadores resumen del reporte (totales, conteos por estado, ingresos...)")
    private Map<String, String> resumen = new LinkedHashMap<>();

    @Schema(description = "Cantidad de filas", example = "25")
    private int totalRegistros;
}

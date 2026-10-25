package presentationLayer.controller;

import bussinesLayer.dto.ReporteArchivoDTO;
import bussinesLayer.dto.ReporteDTO;
import bussinesLayer.service.ReporteService;
import bussinesLayer.util.TipoReporte;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import presentationLayer.advice.ApiError;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**  78: el administrador genera reportes y los exporta en PDF y CSV. */
@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reportes", description = "Reportes de uso, préstamos, reservas, eventos, suscripciones y pedidos interciudad, exportables en PDF y CSV ( 78)")
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/tipos")
    @Operation(summary = "Listar los tipos de reporte disponibles")
    @ApiResponse(responseCode = "200", description = "Tipos de reporte")
    public ResponseEntity<List<String>> tipos() {
        return ResponseEntity.ok(Arrays.stream(TipoReporte.values()).map(Enum::name).toList());
    }

    @GetMapping("/{tipo}")
    @Operation(summary = "Generar un reporte (vista en JSON)",
            description = "Tipos: USO, PRESTAMOS, RESERVAS, EVENTOS, SUSCRIPCIONES, PEDIDOS_INTERCIUDAD. "
                    + "El filtro de sede no aplica a USO ni SUSCRIPCIONES.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte generado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReporteDTO.class))),
            @ApiResponse(responseCode = "400", description = "Tipo de reporte o fechas inválidas",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Sede no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ReporteDTO> generar(
            @Parameter(description = "Tipo de reporte", required = true, example = "PRESTAMOS") @PathVariable String tipo,
            @Parameter(description = "Fecha inicial (inclusive), yyyy-MM-dd", example = "2026-10-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Fecha final (inclusive), yyyy-MM-dd", example = "2026-10-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Filtrar por sede (opcional)", example = "1") @RequestParam(required = false) Long sedeId,
            @Parameter(description = "ID del administrador que genera el reporte (queda en la auditoría)", example = "1")
            @RequestParam(required = false) Long administradorId) {
        log.info("GET /api/v1/reportes/{} - desde={}, hasta={}, sedeId={}", tipo, desde, hasta, sedeId);
        return ResponseEntity.ok(reporteService.generarReporte(tipo, desde, hasta, sedeId, administradorId));
    }

    @GetMapping("/{tipo}/exportar")
    @Operation(summary = "Exportar un reporte en PDF o CSV", description = "Descarga el archivo con el mismo contenido que la vista JSON")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Archivo del reporte",
                    content = {@Content(mediaType = "application/pdf"), @Content(mediaType = "text/csv")}),
            @ApiResponse(responseCode = "400", description = "Tipo, formato o fechas inválidas",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Sede no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<byte[]> exportar(
            @Parameter(description = "Tipo de reporte", required = true, example = "PRESTAMOS") @PathVariable String tipo,
            @Parameter(description = "Formato de exportación: PDF o CSV", required = true, example = "PDF") @RequestParam String formato,
            @Parameter(description = "Fecha inicial (inclusive), yyyy-MM-dd", example = "2026-10-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Fecha final (inclusive), yyyy-MM-dd", example = "2026-10-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Filtrar por sede (opcional)", example = "1") @RequestParam(required = false) Long sedeId,
            @Parameter(description = "ID del administrador que exporta el reporte (queda en la auditoría)", example = "1")
            @RequestParam(required = false) Long administradorId) {
        log.info("GET /api/v1/reportes/{}/exportar - formato={}", tipo, formato);
        ReporteArchivoDTO archivo = reporteService.exportarReporte(tipo, formato, desde, hasta, sedeId, administradorId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(archivo.getNombreArchivo()).build().toString())
                .contentType(MediaType.parseMediaType(archivo.getContentType()))
                .body(archivo.getContenido());
    }
}

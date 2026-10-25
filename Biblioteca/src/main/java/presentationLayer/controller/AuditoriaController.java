package presentationLayer.controller;

import bussinesLayer.dto.AuditoriaDTO;
import bussinesLayer.service.AuditoriaService;
import bussinesLayer.util.AccionAuditoria;
import bussinesLayer.util.ModuloAuditoria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import persistanceLayer.dao.AuditoriaDAO;
import presentationLayer.advice.ApiError;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** RF-79: el administrador consulta el historial de auditoría del sistema. */
@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Auditoría", description = "Consulta del historial de auditoría del sistema (RF-79)")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @GetMapping
    @Operation(summary = "Consultar el historial de auditoría",
            description = "Lista los registros del más reciente al más antiguo. Todos los filtros son opcionales y se pueden combinar. "
                    + "El total de registros que cumplen el filtro se devuelve en el encabezado X-Total-Count.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros de auditoría",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AuditoriaDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Filtros inválidos (rango de fechas, paginación)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<AuditoriaDTO>> consultar(
            @Parameter(description = "Id del usuario que hizo la acción", example = "1") @RequestParam(required = false) Long usuarioId,
            @Parameter(description = "Fecha inicial (inclusive), formato yyyy-MM-dd", example = "2026-10-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Fecha final (inclusive), formato yyyy-MM-dd", example = "2026-10-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Tipo de acción (CREAR, MODIFICAR, DESACTIVAR, REACTIVAR, SUSPENDER, ELIMINAR, CONFIGURAR, GENERAR_REPORTE, EXPORTAR_REPORTE)", example = "MODIFICAR")
            @RequestParam(required = false) String accion,
            @Parameter(description = "Módulo (USUARIOS, PERSONAL, SEDES, PLANES, POLITICAS, TARIFAS_ENVIO, REPORTES, CHATBOT)", example = "SEDES")
            @RequestParam(required = false) String modulo,
            @Parameter(description = "Número de página, desde 0", example = "0") @RequestParam(defaultValue = "0") int pagina,
            @Parameter(description = "Registros por página (1 a 200)", example = "50") @RequestParam(defaultValue = "50") int tamanio) {
        log.info("GET /api/v1/auditoria - usuarioId={}, accion={}, modulo={}", usuarioId, accion, modulo);
        AuditoriaDAO.Pagina resultado = auditoriaService.consultar(usuarioId, desde, hasta, accion, modulo, pagina, tamanio);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(resultado.total()))
                .body(resultado.contenido());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar un registro de auditoría por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuditoriaDTO.class))),
            @ApiResponse(responseCode = "404", description = "Registro no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<AuditoriaDTO> obtenerPorId(
            @Parameter(description = "ID del registro", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(auditoriaService.obtenerPorId(id));
    }

    @GetMapping("/filtros")
    @Operation(summary = "Valores disponibles para los filtros", description = "Devuelve las acciones y módulos que se pueden usar como filtro")
    @ApiResponse(responseCode = "200", description = "Acciones y módulos")
    public ResponseEntity<Map<String, List<String>>> filtrosDisponibles() {
        return ResponseEntity.ok(Map.of("acciones", AccionAuditoria.TODAS, "modulos", ModuloAuditoria.TODOS));
    }
}
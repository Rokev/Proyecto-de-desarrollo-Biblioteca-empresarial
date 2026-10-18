package presentationLayer.controller;

import bussinesLayer.dto.JustificacionRetrasoDTO;
import bussinesLayer.service.JustificacionRetrasoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import presentationLayer.advice.ApiError;

import java.util.List;

@RestController
@RequestMapping("/api/v1/justificaciones-retraso")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Justificaciones de retraso", description = "Operaciones CRUD para la gestion de justificaciones de retraso en devoluciones")
public class JustificacionRetrasoController {

    private final JustificacionRetrasoService justificacionRetrasoService;

    @PostMapping
    @Operation(summary = "Crear una justificacion de retraso", description = "Registra una nueva justificacion de retraso asociada a un prestamo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Justificacion de retraso creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = JustificacionRetrasoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o faltantes",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Prestamo, usuario o bibliotecario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<JustificacionRetrasoDTO> crearJustificacionRetraso(
            @Parameter(description = "Datos de la justificacion de retraso a crear", required = true)
            @Valid @RequestBody JustificacionRetrasoDTO justificacionRetrasoDTO) {
        log.info("POST /api/v1/justificaciones-retraso - prestamoId: {}", justificacionRetrasoDTO.getPrestamoId());
        JustificacionRetrasoDTO creada = justificacionRetrasoService.crearJustificacionRetraso(justificacionRetrasoDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar justificacion de retraso por ID", description = "Obtiene la informacion de una justificacion de retraso especifica")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Justificacion de retraso encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = JustificacionRetrasoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Justificacion de retraso no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<JustificacionRetrasoDTO> obtenerPorId(
            @Parameter(description = "ID de la justificacion de retraso", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/justificaciones-retraso/{}", id);
        return ResponseEntity.ok(justificacionRetrasoService.obtenerJustificacionRetrasoPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todas las justificaciones de retraso", description = "Obtiene la lista completa de justificaciones de retraso registradas")
    @ApiResponse(responseCode = "200", description = "Lista de justificaciones de retraso obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = JustificacionRetrasoDTO.class))))
    public ResponseEntity<List<JustificacionRetrasoDTO>> obtenerTodos() {
        log.debug("GET /api/v1/justificaciones-retraso");
        return ResponseEntity.ok(justificacionRetrasoService.obtenerTodasLasJustificacionesRetraso());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar justificacion de retraso", description = "Actualiza los datos de una justificacion de retraso existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Justificacion de retraso actualizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = JustificacionRetrasoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o faltantes",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Justificacion, prestamo, usuario o bibliotecario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<JustificacionRetrasoDTO> actualizar(
            @Parameter(description = "ID de la justificacion de retraso a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos de la justificacion de retraso", required = true)
            @Valid @RequestBody JustificacionRetrasoDTO justificacionRetrasoDTO) {
        log.info("PUT /api/v1/justificaciones-retraso/{}", id);
        return ResponseEntity.ok(justificacionRetrasoService.actualizarJustificacionRetraso(id, justificacionRetrasoDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar justificacion de retraso", description = "Elimina una justificacion de retraso del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Justificacion de retraso eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Justificacion de retraso no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionada con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID de la justificacion de retraso a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/justificaciones-retraso/{}", id);
        justificacionRetrasoService.eliminarJustificacionRetraso(id);
        return ResponseEntity.noContent().build();
    }
}
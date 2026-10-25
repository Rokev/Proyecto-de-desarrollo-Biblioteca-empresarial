package presentationLayer.controller;
import bussinesLayer.dto.AsistenteEventoDTO;
import org.springframework.web.bind.annotation.RequestParam;

import bussinesLayer.dto.InscripcionEventoDTO;
import bussinesLayer.service.InscripcionEventoService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inscripciones-evento")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inscripciones a eventos", description = "Operaciones CRUD para la gestion de inscripciones de usuarios a eventos")
public class InscripcionEventoController {

    private final InscripcionEventoService inscripcionEventoService;

    @PostMapping
    @Operation(summary = "Crear una inscripcion a evento", description = "Inscribe a un usuario en un evento")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inscripcion a evento creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = InscripcionEventoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, usuario ya inscrito o evento no programado / sin cupos suficientes, o evento exclusivo sin suscripcion vigente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Evento o usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<InscripcionEventoDTO> crearInscripcionEvento(
            @Parameter(description = "Datos de la inscripcion a evento a crear", required = true)
            @Valid @RequestBody InscripcionEventoDTO inscripcionEventoDTO) {
        log.info("POST /api/v1/inscripciones-evento - eventoId: {}, usuarioId: {}", inscripcionEventoDTO.getEventoId(), inscripcionEventoDTO.getUsuarioId());
        InscripcionEventoDTO creada = inscripcionEventoService.crearInscripcionEvento(inscripcionEventoDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar inscripcion a evento por ID", description = "Obtiene la informacion de una inscripcion a evento especifica")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inscripcion a evento encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = InscripcionEventoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Inscripcion a evento no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<InscripcionEventoDTO> obtenerPorId(
            @Parameter(description = "ID de la inscripcion a evento", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/inscripciones-evento/{}", id);
        return ResponseEntity.ok(inscripcionEventoService.obtenerInscripcionEventoPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todas las inscripciones a eventos", description = "Obtiene la lista completa de inscripciones a eventos registradas")
    @ApiResponse(responseCode = "200", description = "Lista de inscripciones a eventos obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = InscripcionEventoDTO.class))))
    public ResponseEntity<List<InscripcionEventoDTO>> obtenerTodos() {
        log.debug("GET /api/v1/inscripciones-evento");
        return ResponseEntity.ok(inscripcionEventoService.obtenerTodasLasInscripcionesEvento());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar inscripcion a evento", description = "Actualiza los datos de una inscripcion a evento existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inscripcion a evento actualizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = InscripcionEventoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, intento de cambiar evento o usuario, o evento sin cupos suficientes",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Inscripcion, evento o usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<InscripcionEventoDTO> actualizar(
            @Parameter(description = "ID de la inscripcion a evento a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos de la inscripcion a evento", required = true)
            @Valid @RequestBody InscripcionEventoDTO inscripcionEventoDTO) {
        log.info("PUT /api/v1/inscripciones-evento/{}", id);
        return ResponseEntity.ok(inscripcionEventoService.actualizarInscripcionEvento(id, inscripcionEventoDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar inscripcion a evento", description = "Elimina una inscripcion a evento del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Inscripcion a evento eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Inscripcion a evento no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionada con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID de la inscripcion a evento a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/inscripciones-evento/{}", id);
        inscripcionEventoService.eliminarInscripcionEvento(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/depurar-sin-suscripcion")
    @Operation(summary = "Cancelar inscripciones exclusivas sin suscripcion vigente (administrativo)",
            description = "Cancela las inscripciones a eventos exclusivos futuros de usuarios sin suscripcion vigente y libera sus cupos. "
                    + "Tambien se ejecuta automaticamente cada noche.")
    @ApiResponse(responseCode = "200", description = "Cantidad de inscripciones canceladas")
    public ResponseEntity<Map<String, Integer>> depurarSinSuscripcion() {
        log.info("POST /api/v1/inscripciones-evento/depurar-sin-suscripcion");
        int canceladas = inscripcionEventoService.cancelarInscripcionesExclusivasSinSuscripcion();
        return ResponseEntity.ok(Map.of("inscripcionesCanceladas", canceladas));
    }

    @GetMapping("/evento/{eventoId}")
    @Operation(summary = "Consultar asistentes de un evento", description = "Lista actualizada de usuarios inscritos al evento (por defecto solo inscripciones ACTIVAS; estado=TODAS para ver todas)")
    @ApiResponse(responseCode = "200", description = "Consulta realizada exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AsistenteEventoDTO.class))))
    public ResponseEntity<List<AsistenteEventoDTO>> obtenerAsistentes(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId,
            @Parameter(description = "Estado de la inscripcion (ACTIVA por defecto, TODAS para todas)") @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(inscripcionEventoService.obtenerAsistentesDeEvento(eventoId, estado));
    }
}

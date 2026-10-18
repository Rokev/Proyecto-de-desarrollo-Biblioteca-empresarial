package presentationLayer.controller;

import bussinesLayer.dto.PreguntaFrecuenteDTO;
import bussinesLayer.service.PreguntaFrecuenteService;
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
@RequestMapping("/api/v1/preguntas-frecuentes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Preguntas frecuentes", description = "Operaciones CRUD para la gestion de preguntas frecuentes")
public class PreguntaFrecuenteController {

    private final PreguntaFrecuenteService preguntaFrecuenteService;

    @PostMapping
    @Operation(summary = "Crear una pregunta frecuente", description = "Registra una nueva pregunta frecuente")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pregunta frecuente creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PreguntaFrecuenteDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o faltantes",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PreguntaFrecuenteDTO> crearPreguntaFrecuente(
            @Parameter(description = "Datos de la pregunta frecuente a crear", required = true)
            @Valid @RequestBody PreguntaFrecuenteDTO preguntaFrecuenteDTO) {
        log.info("POST /api/v1/preguntas-frecuentes - categoria: {}", preguntaFrecuenteDTO.getCategoria());
        PreguntaFrecuenteDTO creada = preguntaFrecuenteService.crearPreguntaFrecuente(preguntaFrecuenteDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar pregunta frecuente por ID", description = "Obtiene la informacion de una pregunta frecuente especifica")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pregunta frecuente encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PreguntaFrecuenteDTO.class))),
            @ApiResponse(responseCode = "404", description = "Pregunta frecuente no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PreguntaFrecuenteDTO> obtenerPorId(
            @Parameter(description = "ID de la pregunta frecuente", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/preguntas-frecuentes/{}", id);
        return ResponseEntity.ok(preguntaFrecuenteService.obtenerPreguntaFrecuentePorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todas las preguntas frecuentes", description = "Obtiene la lista completa de preguntas frecuentes registradas")
    @ApiResponse(responseCode = "200", description = "Lista de preguntas frecuentes obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PreguntaFrecuenteDTO.class))))
    public ResponseEntity<List<PreguntaFrecuenteDTO>> obtenerTodos() {
        log.debug("GET /api/v1/preguntas-frecuentes");
        return ResponseEntity.ok(preguntaFrecuenteService.obtenerTodasLasPreguntasFrecuentes());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar pregunta frecuente", description = "Actualiza los datos de una pregunta frecuente existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pregunta frecuente actualizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PreguntaFrecuenteDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o faltantes",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pregunta frecuente no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PreguntaFrecuenteDTO> actualizar(
            @Parameter(description = "ID de la pregunta frecuente a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos de la pregunta frecuente", required = true)
            @Valid @RequestBody PreguntaFrecuenteDTO preguntaFrecuenteDTO) {
        log.info("PUT /api/v1/preguntas-frecuentes/{}", id);
        return ResponseEntity.ok(preguntaFrecuenteService.actualizarPreguntaFrecuente(id, preguntaFrecuenteDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar pregunta frecuente", description = "Elimina una pregunta frecuente del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pregunta frecuente eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Pregunta frecuente no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionada con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID de la pregunta frecuente a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/preguntas-frecuentes/{}", id);
        preguntaFrecuenteService.eliminarPreguntaFrecuente(id);
        return ResponseEntity.noContent().build();
    }
}
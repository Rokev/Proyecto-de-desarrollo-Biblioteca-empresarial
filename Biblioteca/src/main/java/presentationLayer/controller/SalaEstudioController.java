package presentationLayer.controller;

import bussinesLayer.dto.SalaEstudioDTO;
import bussinesLayer.service.SalaEstudioService;
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
@RequestMapping("/api/v1/salas-estudio")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Salas de estudio", description = "Operaciones CRUD para la gestion de salas de estudio de las sedes")
public class SalaEstudioController {

    private final SalaEstudioService salaEstudioService;

    @PostMapping
    @Operation(summary = "Crear una sala de estudio", description = "Registra una nueva sala de estudio en una sede")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sala de estudio creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SalaEstudioDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, capacidad menor a 1 o nombre de sala ya registrado en la sede",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Sede no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SalaEstudioDTO> crearSalaEstudio(
            @Parameter(description = "Datos de la sala de estudio a crear", required = true)
            @Valid @RequestBody SalaEstudioDTO salaEstudioDTO) {
        log.info("POST /api/v1/salas-estudio - nombre: {}, sedeId: {}", salaEstudioDTO.getNombre(), salaEstudioDTO.getSedeId());
        SalaEstudioDTO creada = salaEstudioService.crearSalaEstudio(salaEstudioDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar sala de estudio por ID", description = "Obtiene la informacion de una sala de estudio especifica")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sala de estudio encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SalaEstudioDTO.class))),
            @ApiResponse(responseCode = "404", description = "Sala de estudio no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SalaEstudioDTO> obtenerPorId(
            @Parameter(description = "ID de la sala de estudio", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/salas-estudio/{}", id);
        return ResponseEntity.ok(salaEstudioService.obtenerSalaEstudioPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todas las salas de estudio", description = "Obtiene la lista completa de salas de estudio registradas")
    @ApiResponse(responseCode = "200", description = "Lista de salas de estudio obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SalaEstudioDTO.class))))
    public ResponseEntity<List<SalaEstudioDTO>> obtenerTodos() {
        log.debug("GET /api/v1/salas-estudio");
        return ResponseEntity.ok(salaEstudioService.obtenerTodasLasSalasEstudio());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar sala de estudio", description = "Actualiza los datos de una sala de estudio existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sala de estudio actualizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SalaEstudioDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, capacidad menor a 1 o nombre de sala ya registrado en la sede",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Sala de estudio o sede no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SalaEstudioDTO> actualizar(
            @Parameter(description = "ID de la sala de estudio a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos de la sala de estudio", required = true)
            @Valid @RequestBody SalaEstudioDTO salaEstudioDTO) {
        log.info("PUT /api/v1/salas-estudio/{}", id);
        return ResponseEntity.ok(salaEstudioService.actualizarSalaEstudio(id, salaEstudioDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar sala de estudio", description = "Elimina una sala de estudio del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sala de estudio eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Sala de estudio no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionada con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID de la sala de estudio a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/salas-estudio/{}", id);
        salaEstudioService.eliminarSalaEstudio(id);
        return ResponseEntity.noContent().build();
    }
}
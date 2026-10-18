package presentationLayer.controller;

import bussinesLayer.dto.SuscripcionDTO;
import bussinesLayer.service.SuscripcionService;
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
@RequestMapping("/api/v1/suscripciones")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Suscripciones", description = "Operaciones CRUD para la gestion de suscripciones de usuarios a un plan")
public class SuscripcionController {

    private final SuscripcionService suscripcionService;

    @PostMapping
    @Operation(summary = "Crear una suscripcion", description = "Registra una nueva suscripcion de un usuario a un plan")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Suscripcion creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SuscripcionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, fechas inconsistentes, plan inactivo o el usuario ya tiene una suscripcion activa",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario o plan de suscripcion no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SuscripcionDTO> crearSuscripcion(
            @Parameter(description = "Datos de la suscripcion a crear", required = true)
            @Valid @RequestBody SuscripcionDTO suscripcionDTO) {
        log.info("POST /api/v1/suscripciones - usuarioId: {}, planId: {}", suscripcionDTO.getUsuarioId(), suscripcionDTO.getPlanId());
        SuscripcionDTO creada = suscripcionService.crearSuscripcion(suscripcionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar suscripcion por ID", description = "Obtiene la informacion de una suscripcion especifica")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Suscripcion encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SuscripcionDTO.class))),
            @ApiResponse(responseCode = "404", description = "Suscripcion no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SuscripcionDTO> obtenerPorId(
            @Parameter(description = "ID de la suscripcion", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/suscripciones/{}", id);
        return ResponseEntity.ok(suscripcionService.obtenerSuscripcionPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todas las suscripciones", description = "Obtiene la lista completa de suscripciones registradas")
    @ApiResponse(responseCode = "200", description = "Lista de suscripciones obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SuscripcionDTO.class))))
    public ResponseEntity<List<SuscripcionDTO>> obtenerTodos() {
        log.debug("GET /api/v1/suscripciones");
        return ResponseEntity.ok(suscripcionService.obtenerTodasLasSuscripciones());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar suscripcion", description = "Actualiza los datos de una suscripcion existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Suscripcion actualizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SuscripcionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, fechas inconsistentes, plan inactivo o intento de cambiar el usuario",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Suscripcion, usuario o plan de suscripcion no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SuscripcionDTO> actualizar(
            @Parameter(description = "ID de la suscripcion a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos de la suscripcion", required = true)
            @Valid @RequestBody SuscripcionDTO suscripcionDTO) {
        log.info("PUT /api/v1/suscripciones/{}", id);
        return ResponseEntity.ok(suscripcionService.actualizarSuscripcion(id, suscripcionDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar suscripcion", description = "Elimina una suscripcion del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Suscripcion eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Suscripcion no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionada con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID de la suscripcion a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/suscripciones/{}", id);
        suscripcionService.eliminarSuscripcion(id);
        return ResponseEntity.noContent().build();
    }
}
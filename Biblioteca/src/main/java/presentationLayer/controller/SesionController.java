package presentationLayer.controller;

import bussinesLayer.dto.SesionDTO;
import bussinesLayer.service.SesionService;
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
@RequestMapping("/api/v1/sesiones")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sesiones", description = "Operaciones CRUD para la gestion de sesiones de usuario")
public class SesionController {

    private final SesionService sesionService;

    @PostMapping
    @Operation(summary = "Crear una sesion", description = "Registra una nueva sesion de un usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sesion creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SesionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, token de sesion ya existente o fecha de expiracion anterior al inicio",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SesionDTO> crearSesion(
            @Parameter(description = "Datos de la sesion a crear", required = true)
            @Valid @RequestBody SesionDTO sesionDTO) {
        log.info("POST /api/v1/sesiones - usuarioId: {}", sesionDTO.getUsuarioId());
        SesionDTO creada = sesionService.crearSesion(sesionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar sesion por ID", description = "Obtiene la informacion de una sesion especifica")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesion encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SesionDTO.class))),
            @ApiResponse(responseCode = "404", description = "Sesion no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SesionDTO> obtenerPorId(
            @Parameter(description = "ID de la sesion", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/sesiones/{}", id);
        return ResponseEntity.ok(sesionService.obtenerSesionPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todas las sesiones", description = "Obtiene la lista completa de sesiones registradas")
    @ApiResponse(responseCode = "200", description = "Lista de sesiones obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SesionDTO.class))))
    public ResponseEntity<List<SesionDTO>> obtenerTodos() {
        log.debug("GET /api/v1/sesiones");
        return ResponseEntity.ok(sesionService.obtenerTodasLasSesiones());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar sesion", description = "Actualiza los datos de una sesion existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesion actualizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SesionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, token de sesion ya existente o fecha de expiracion anterior al inicio",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Sesion o usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SesionDTO> actualizar(
            @Parameter(description = "ID de la sesion a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos de la sesion", required = true)
            @Valid @RequestBody SesionDTO sesionDTO) {
        log.info("PUT /api/v1/sesiones/{}", id);
        return ResponseEntity.ok(sesionService.actualizarSesion(id, sesionDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar sesion", description = "Elimina una sesion del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sesion eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Sesion no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionada con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID de la sesion a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/sesiones/{}", id);
        sesionService.eliminarSesion(id);
        return ResponseEntity.noContent().build();
    }
}
package presentationLayer.controller;
import org.springframework.web.bind.annotation.RequestParam;

import bussinesLayer.dto.SancionDTO;
import bussinesLayer.service.SancionService;
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
@RequestMapping("/api/v1/sanciones")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sanciones", description = "Operaciones CRUD para la gestion de sanciones a usuarios")
public class SancionController {

    private final SancionService sancionService;

    @PostMapping
    @Operation(summary = "Crear una sancion", description = "Registra una nueva sancion a un usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sancion creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SancionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, fechas inconsistentes o el usuario ya tiene una sancion activa del mismo tipo",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario o prestamo no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SancionDTO> crearSancion(
            @Parameter(description = "Datos de la sancion a crear", required = true)
            @Valid @RequestBody SancionDTO sancionDTO) {
        log.info("POST /api/v1/sanciones - usuarioId: {}", sancionDTO.getUsuarioId());
        SancionDTO creada = sancionService.crearSancion(sancionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar sancion por ID", description = "Obtiene la informacion de una sancion especifica")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sancion encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SancionDTO.class))),
            @ApiResponse(responseCode = "404", description = "Sancion no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SancionDTO> obtenerPorId(
            @Parameter(description = "ID de la sancion", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/sanciones/{}", id);
        return ResponseEntity.ok(sancionService.obtenerSancionPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todas las sanciones", description = "Obtiene la lista completa de sanciones registradas")
    @ApiResponse(responseCode = "200", description = "Lista de sanciones obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SancionDTO.class))))
    public ResponseEntity<List<SancionDTO>> obtenerTodos() {
        log.debug("GET /api/v1/sanciones");
        return ResponseEntity.ok(sancionService.obtenerTodasLasSanciones());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar sancion", description = "Actualiza los datos de una sancion existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sancion actualizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SancionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, fechas inconsistentes o sancion activa del mismo tipo",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Sancion, usuario o prestamo no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SancionDTO> actualizar(
            @Parameter(description = "ID de la sancion a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos de la sancion", required = true)
            @Valid @RequestBody SancionDTO sancionDTO) {
        log.info("PUT /api/v1/sanciones/{}", id);
        return ResponseEntity.ok(sancionService.actualizarSancion(id, sancionDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar sancion", description = "Elimina una sancion del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sancion eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Sancion no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionada con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID de la sancion a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/sanciones/{}", id);
        sancionService.eliminarSancion(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = "Consultar sanciones de un usuario", description = "Lista las sanciones del usuario con motivo y fecha de aplicacion (por defecto solo las ACTIVAS; use estado=TODAS para ver todas)")
    @ApiResponse(responseCode = "200", description = "Consulta realizada exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SancionDTO.class))))
    public ResponseEntity<List<SancionDTO>> obtenerPorUsuario(
            @Parameter(description = "ID del usuario", example = "1") @PathVariable Long usuarioId,
            @Parameter(description = "Estado de la sancion (ACTIVA por defecto, TODAS para todas)") @RequestParam(defaultValue = "ACTIVA") String estado) {
        return ResponseEntity.ok(sancionService.obtenerSancionesDeUsuario(usuarioId, estado));
    }
}
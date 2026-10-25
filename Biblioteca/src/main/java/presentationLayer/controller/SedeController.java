package presentationLayer.controller;
import org.springframework.web.bind.annotation.RequestParam;

import bussinesLayer.dto.SedeDTO;
import bussinesLayer.service.SedeService;
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
@RequestMapping("/api/v1/sedes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sedes", description = "Operaciones CRUD para la gestion de sedes de la biblioteca")
class SedeController {

    private final SedeService sedeService;

    @PostMapping
    @Operation(summary = "Crear una sede", description = "Registra una nueva sede de la biblioteca")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sede creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SedeDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, nombre de sede ya registrado, horario inconsistente o latitud y longitud incompletas",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SedeDTO> crearSede(
            @Parameter(description = "Datos de la sede a crear", required = true)
            @Valid @RequestBody SedeDTO sedeDTO) {
        log.info("POST /api/v1/sedes - nombre: {}", sedeDTO.getNombre());
        SedeDTO creada = sedeService.crearSede(sedeDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar sede por ID", description = "Obtiene la informacion de una sede especifica")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sede encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SedeDTO.class))),
            @ApiResponse(responseCode = "404", description = "Sede no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SedeDTO> obtenerPorId(
            @Parameter(description = "ID de la sede", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/sedes/{}", id);
        return ResponseEntity.ok(sedeService.obtenerSedePorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todas las sedes", description = "Obtiene la lista completa de sedes registradas")
    @ApiResponse(responseCode = "200", description = "Lista de sedes obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SedeDTO.class))))
    public ResponseEntity<List<SedeDTO>> obtenerTodos() {
        log.debug("GET /api/v1/sedes");
        return ResponseEntity.ok(sedeService.obtenerTodasLasSedes());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar sede", description = "Actualiza los datos de una sede existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sede actualizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SedeDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, nombre de sede ya registrado por otra sede, horario inconsistente o latitud y longitud incompletas",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Sede no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SedeDTO> actualizar(
            @Parameter(description = "ID de la sede a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos de la sede", required = true)
            @Valid @RequestBody SedeDTO sedeDTO) {
        log.info("PUT /api/v1/sedes/{}", id);
        return ResponseEntity.ok(sedeService.actualizarSede(id, sedeDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar sede", description = "Elimina una sede del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sede eliminada exitosamente"),
            @ApiResponse(responseCode = "400", description = "No se puede eliminar la sede porque tiene ejemplares asociados",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Sede no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionada con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID de la sede a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/sedes/{}", id);
        sedeService.eliminarSede(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/activas")
    @Operation(summary = "Listar sedes activas", description = "Obtiene unicamente las sedes en operacion (estado activo)")
    @ApiResponse(responseCode = "200", description = "Consulta realizada exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SedeDTO.class))))
    public ResponseEntity<List<SedeDTO>> obtenerActivas() {
        return ResponseEntity.ok(sedeService.obtenerSedesActivas());
    }

    @PostMapping("/{id}/desactivar")
    @Operation(summary = "Desactivar sede", description = "Pone la sede fuera de servicio solo si no tiene prestamos activos, reservas pendientes ni pedidos interciudad en curso; si los tiene, la bloquea y notifica al administrador ( 68)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operacion realizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SedeDTO.class))),
            @ApiResponse(responseCode = "400", description = "La operacion no es valida en el estado actual",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SedeDTO> desactivar(
            @Parameter(description = "ID de la sede", example = "1") @PathVariable Long id,
            @Parameter(description = "ID del administrador que solicita la accion (para notificarle si se bloquea)") @RequestParam(required = false) Long administradorId) {
        return ResponseEntity.ok(sedeService.desactivarSede(id, administradorId));
    }

    @PostMapping("/{id}/reactivar")
    @Operation(summary = "Reactivar sede", description = "Cambia la sede de fuera de servicio a activa y vuelve a mostrarla a los usuarios ( 70)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operacion realizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SedeDTO.class))),
            @ApiResponse(responseCode = "400", description = "La operacion no es valida en el estado actual",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SedeDTO> reactivar(
            @Parameter(description = "ID de la sede", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(sedeService.reactivarSede(id));
    }
}
package presentationLayer.controller;

import bussinesLayer.dto.NotificacionDTO;
import bussinesLayer.service.NotificacionService;
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
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notificaciones", description = "Operaciones CRUD para la gestion de notificaciones de los usuarios")
public class NotificacionController {

    private final NotificacionService notificacionService;

    @PostMapping
    @Operation(summary = "Crear una notificacion", description = "Registra una nueva notificacion dirigida a un usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Notificacion creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificacionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o faltantes",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<NotificacionDTO> crearNotificacion(
            @Parameter(description = "Datos de la notificacion a crear", required = true)
            @Valid @RequestBody NotificacionDTO notificacionDTO) {
        log.info("POST /api/v1/notificaciones - usuarioId: {}", notificacionDTO.getUsuarioId());
        NotificacionDTO creada = notificacionService.crearNotificacion(notificacionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar notificacion por ID", description = "Obtiene la informacion de una notificacion especifica")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificacion encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificacionDTO.class))),
            @ApiResponse(responseCode = "404", description = "Notificacion no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<NotificacionDTO> obtenerPorId(
            @Parameter(description = "ID de la notificacion", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/notificaciones/{}", id);
        return ResponseEntity.ok(notificacionService.obtenerNotificacionPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todas las notificaciones", description = "Obtiene la lista completa de notificaciones registradas")
    @ApiResponse(responseCode = "200", description = "Lista de notificaciones obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = NotificacionDTO.class))))
    public ResponseEntity<List<NotificacionDTO>> obtenerTodos() {
        log.debug("GET /api/v1/notificaciones");
        return ResponseEntity.ok(notificacionService.obtenerTodasLasNotificaciones());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar notificacion", description = "Actualiza los datos de una notificacion existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificacion actualizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificacionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o faltantes",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Notificacion o usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<NotificacionDTO> actualizar(
            @Parameter(description = "ID de la notificacion a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos de la notificacion", required = true)
            @Valid @RequestBody NotificacionDTO notificacionDTO) {
        log.info("PUT /api/v1/notificaciones/{}", id);
        return ResponseEntity.ok(notificacionService.actualizarNotificacion(id, notificacionDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar notificacion", description = "Elimina una notificacion del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notificacion eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Notificacion no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionada con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID de la notificacion a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/notificaciones/{}", id);
        notificacionService.eliminarNotificacion(id);
        return ResponseEntity.noContent().build();
    }
}
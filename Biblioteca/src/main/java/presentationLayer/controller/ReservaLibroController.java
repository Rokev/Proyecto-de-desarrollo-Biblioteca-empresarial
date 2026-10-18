package presentationLayer.controller;

import bussinesLayer.dto.ReservaLibroDTO;
import bussinesLayer.service.ReservaLibroService;
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
@RequestMapping("/api/v1/reservas-libro")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reservas de libro", description = "Operaciones CRUD para la gestion de reservas de libros")
public class ReservaLibroController {

    private final ReservaLibroService reservaLibroService;

    @PostMapping
    @Operation(summary = "Crear una reserva de libro", description = "Registra una nueva reserva de un libro para un usuario en una sede")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva de libro creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReservaLibroDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, faltantes o el usuario ya tiene una reserva vigente para el libro",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario, libro, sede o ejemplar no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ReservaLibroDTO> crearReservaLibro(
            @Parameter(description = "Datos de la reserva de libro a crear", required = true)
            @Valid @RequestBody ReservaLibroDTO reservaLibroDTO) {
        log.info("POST /api/v1/reservas-libro - usuarioId: {}, libroId: {}", reservaLibroDTO.getUsuarioId(), reservaLibroDTO.getLibroId());
        ReservaLibroDTO creada = reservaLibroService.crearReservaLibro(reservaLibroDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar reserva de libro por ID", description = "Obtiene la informacion de una reserva de libro especifica")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva de libro encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReservaLibroDTO.class))),
            @ApiResponse(responseCode = "404", description = "Reserva de libro no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ReservaLibroDTO> obtenerPorId(
            @Parameter(description = "ID de la reserva de libro", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/reservas-libro/{}", id);
        return ResponseEntity.ok(reservaLibroService.obtenerReservaLibroPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todas las reservas de libro", description = "Obtiene la lista completa de reservas de libro registradas")
    @ApiResponse(responseCode = "200", description = "Lista de reservas de libro obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservaLibroDTO.class))))
    public ResponseEntity<List<ReservaLibroDTO>> obtenerTodos() {
        log.debug("GET /api/v1/reservas-libro");
        return ResponseEntity.ok(reservaLibroService.obtenerTodasLasReservasLibro());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar reserva de libro", description = "Actualiza los datos de una reserva de libro existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva de libro actualizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReservaLibroDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o intento de cambiar el usuario o el libro de la reserva",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Reserva de libro, usuario, libro, sede o ejemplar no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ReservaLibroDTO> actualizar(
            @Parameter(description = "ID de la reserva de libro a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos de la reserva de libro", required = true)
            @Valid @RequestBody ReservaLibroDTO reservaLibroDTO) {
        log.info("PUT /api/v1/reservas-libro/{}", id);
        return ResponseEntity.ok(reservaLibroService.actualizarReservaLibro(id, reservaLibroDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar reserva de libro", description = "Elimina una reserva de libro del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reserva de libro eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Reserva de libro no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionada con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID de la reserva de libro a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/reservas-libro/{}", id);
        reservaLibroService.eliminarReservaLibro(id);
        return ResponseEntity.noContent().build();
    }
}
package presentationLayer.controller;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.RequestParam;

import bussinesLayer.dto.EventoDTO;
import bussinesLayer.service.EventoService;
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
@RequestMapping("/api/v1/eventos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Eventos", description = "Operaciones CRUD para la gestion de eventos de las sedes")
public class EventoController {

    private final EventoService eventoService;

    @PostMapping
    @Operation(summary = "Crear un evento", description = "Registra un nuevo evento en una sede")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Evento creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, faltantes, fechas inconsistentes o cupos disponibles mayores al cupo maximo",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Sede no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<EventoDTO> crearEvento(
            @Parameter(description = "Datos del evento a crear", required = true)
            @Valid @RequestBody EventoDTO eventoDTO) {
        log.info("POST /api/v1/eventos - titulo: {}", eventoDTO.getTitulo());
        EventoDTO creado = eventoService.crearEvento(eventoDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar evento por ID", description = "Obtiene la informacion de un evento especifico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<EventoDTO> obtenerPorId(
            @Parameter(description = "ID del evento", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/eventos/{}", id);
        return ResponseEntity.ok(eventoService.obtenerEventoPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todos los eventos", description = "Obtiene la lista completa de eventos registrados")
    @ApiResponse(responseCode = "200", description = "Lista de eventos obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = EventoDTO.class))))
    public ResponseEntity<List<EventoDTO>> obtenerTodos() {
        log.debug("GET /api/v1/eventos");
        return ResponseEntity.ok(eventoService.obtenerTodosLosEventos());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar evento", description = "Actualiza los datos de un evento existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, fechas inconsistentes o cupos disponibles mayores al cupo maximo",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Evento o sede no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<EventoDTO> actualizar(
            @Parameter(description = "ID del evento a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos del evento", required = true)
            @Valid @RequestBody EventoDTO eventoDTO) {
        log.info("PUT /api/v1/eventos/{}", id);
        return ResponseEntity.ok(eventoService.actualizarEvento(id, eventoDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar evento", description = "Elimina un evento del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Evento eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionado con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del evento a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/eventos/{}", id);
        eventoService.eliminarEvento(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/proximos")
    @Operation(summary = "Listar eventos proximos", description = "Eventos programados que aun no han empezado (excluye cancelados), filtrables por sede, fecha y tipo")
    @ApiResponse(responseCode = "200", description = "Consulta realizada exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = EventoDTO.class))))
    public ResponseEntity<List<EventoDTO>> buscarProximos(
            @Parameter(description = "Filtro por sede") @RequestParam(required = false) Long sedeId,
            @Parameter(description = "Filtro por fecha de inicio (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @Parameter(description = "true: solo exclusivos; false: solo generales") @RequestParam(required = false) Boolean exclusivo) {
        return ResponseEntity.ok(eventoService.buscarEventosProximos(sedeId, fecha, exclusivo));
    }
}
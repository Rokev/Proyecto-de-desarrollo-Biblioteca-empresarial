package presentationLayer.controller;

import bussinesLayer.dto.DevolucionDTO;
import bussinesLayer.service.DevolucionService;
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
@RequestMapping("/api/v1/devoluciones")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Devoluciones", description = "Operaciones CRUD para la gestion de devoluciones de prestamos")
public class DevolucionController {

    private final DevolucionService devolucionService;

    @PostMapping
    @Operation(summary = "Crear una devolucion", description = "Registra una nueva devolucion asociada a un prestamo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Devolucion creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DevolucionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, faltantes, prestamo con devolucion existente o codigo QR duplicado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Prestamo, sede o bibliotecario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<DevolucionDTO> crearDevolucion(
            @Parameter(description = "Datos de la devolucion a crear", required = true)
            @Valid @RequestBody DevolucionDTO devolucionDTO) {
        log.info("POST /api/v1/devoluciones - prestamoId: {}", devolucionDTO.getPrestamoId());
        DevolucionDTO creada = devolucionService.crearDevolucion(devolucionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar devolucion por ID", description = "Obtiene la informacion de una devolucion especifica")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Devolucion encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DevolucionDTO.class))),
            @ApiResponse(responseCode = "404", description = "Devolucion no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<DevolucionDTO> obtenerPorId(
            @Parameter(description = "ID de la devolucion", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/devoluciones/{}", id);
        return ResponseEntity.ok(devolucionService.obtenerDevolucionPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todas las devoluciones", description = "Obtiene la lista completa de devoluciones registradas")
    @ApiResponse(responseCode = "200", description = "Lista de devoluciones obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = DevolucionDTO.class))))
    public ResponseEntity<List<DevolucionDTO>> obtenerTodos() {
        log.debug("GET /api/v1/devoluciones");
        return ResponseEntity.ok(devolucionService.obtenerTodasLasDevoluciones());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar devolucion", description = "Actualiza los datos de una devolucion existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Devolucion actualizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DevolucionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, prestamo con otra devolucion o codigo QR ya registrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Devolucion, prestamo, sede o bibliotecario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<DevolucionDTO> actualizar(
            @Parameter(description = "ID de la devolucion a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos de la devolucion", required = true)
            @Valid @RequestBody DevolucionDTO devolucionDTO) {
        log.info("PUT /api/v1/devoluciones/{}", id);
        return ResponseEntity.ok(devolucionService.actualizarDevolucion(id, devolucionDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar devolucion", description = "Elimina una devolucion del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Devolucion eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Devolucion no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionada con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID de la devolucion a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/devoluciones/{}", id);
        devolucionService.eliminarDevolucion(id);
        return ResponseEntity.noContent().build();
    }
}
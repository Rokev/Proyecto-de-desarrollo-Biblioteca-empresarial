package presentationLayer.controller;

import bussinesLayer.dto.MultaDTO;
import bussinesLayer.service.MultaService;
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
@RequestMapping("/api/v1/multas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Multas", description = "Operaciones CRUD para la gestion de multas de los usuarios")
public class MultaController {

    private final MultaService multaService;

    @PostMapping
    @Operation(summary = "Crear una multa", description = "Registra una nueva multa asociada a un usuario y a un prestamo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Multa creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MultaDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o faltantes",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario o prestamo no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<MultaDTO> crearMulta(
            @Parameter(description = "Datos de la multa a crear", required = true)
            @Valid @RequestBody MultaDTO multaDTO) {
        log.info("POST /api/v1/multas - usuarioId: {}", multaDTO.getUsuarioId());
        MultaDTO creada = multaService.crearMulta(multaDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar multa por ID", description = "Obtiene la informacion de una multa especifica")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Multa encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MultaDTO.class))),
            @ApiResponse(responseCode = "404", description = "Multa no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<MultaDTO> obtenerPorId(
            @Parameter(description = "ID de la multa", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/multas/{}", id);
        return ResponseEntity.ok(multaService.obtenerMultaPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todas las multas", description = "Obtiene la lista completa de multas registradas")
    @ApiResponse(responseCode = "200", description = "Lista de multas obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = MultaDTO.class))))
    public ResponseEntity<List<MultaDTO>> obtenerTodos() {
        log.debug("GET /api/v1/multas");
        return ResponseEntity.ok(multaService.obtenerTodasLasMultas());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar multa", description = "Actualiza los datos de una multa existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Multa actualizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MultaDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o faltantes",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Multa, usuario o prestamo no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<MultaDTO> actualizar(
            @Parameter(description = "ID de la multa a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos de la multa", required = true)
            @Valid @RequestBody MultaDTO multaDTO) {
        log.info("PUT /api/v1/multas/{}", id);
        return ResponseEntity.ok(multaService.actualizarMulta(id, multaDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar multa", description = "Elimina una multa del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Multa eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Multa no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionada con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID de la multa a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/multas/{}", id);
        multaService.eliminarMulta(id);
        return ResponseEntity.noContent().build();
    }
}
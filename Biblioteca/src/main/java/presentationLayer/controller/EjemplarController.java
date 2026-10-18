package presentationLayer.controller;

import bussinesLayer.dto.EjemplarDTO;
import bussinesLayer.service.EjemplarService;
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
@RequestMapping("/api/v1/ejemplares")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ejemplares", description = "Operaciones CRUD para la gestion de ejemplares (copias fisicas) de los libros")
public class EjemplarController {

    private final EjemplarService ejemplarService;

    @PostMapping
    @Operation(summary = "Crear un ejemplar", description = "Registra un nuevo ejemplar de un libro en una sede")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ejemplar creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EjemplarDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, faltantes o codigo de barras ya registrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Libro o sede no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<EjemplarDTO> crearEjemplar(
            @Parameter(description = "Datos del ejemplar a crear", required = true)
            @Valid @RequestBody EjemplarDTO ejemplarDTO) {
        log.info("POST /api/v1/ejemplares - codigoBarras: {}", ejemplarDTO.getCodigoBarras());
        EjemplarDTO creado = ejemplarService.crearEjemplar(ejemplarDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar ejemplar por ID", description = "Obtiene la informacion de un ejemplar especifico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ejemplar encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EjemplarDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ejemplar no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<EjemplarDTO> obtenerPorId(
            @Parameter(description = "ID del ejemplar", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/ejemplares/{}", id);
        return ResponseEntity.ok(ejemplarService.obtenerEjemplarPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todos los ejemplares", description = "Obtiene la lista completa de ejemplares registrados")
    @ApiResponse(responseCode = "200", description = "Lista de ejemplares obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = EjemplarDTO.class))))
    public ResponseEntity<List<EjemplarDTO>> obtenerTodos() {
        log.debug("GET /api/v1/ejemplares");
        return ResponseEntity.ok(ejemplarService.obtenerTodosLosEjemplares());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar ejemplar", description = "Actualiza los datos de un ejemplar existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ejemplar actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EjemplarDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o codigo de barras ya registrado por otro ejemplar",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Ejemplar, libro o sede no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<EjemplarDTO> actualizar(
            @Parameter(description = "ID del ejemplar a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos del ejemplar", required = true)
            @Valid @RequestBody EjemplarDTO ejemplarDTO) {
        log.info("PUT /api/v1/ejemplares/{}", id);
        return ResponseEntity.ok(ejemplarService.actualizarEjemplar(id, ejemplarDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar ejemplar", description = "Elimina un ejemplar del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ejemplar eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Ejemplar no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionado con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del ejemplar a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/ejemplares/{}", id);
        ejemplarService.eliminarEjemplar(id);
        return ResponseEntity.noContent().build();
    }
}
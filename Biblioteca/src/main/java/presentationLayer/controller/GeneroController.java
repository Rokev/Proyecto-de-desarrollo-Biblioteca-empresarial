package presentationLayer.controller;

import bussinesLayer.dto.GeneroDTO;
import bussinesLayer.service.GeneroService;
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
@RequestMapping("/api/v1/generos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Generos", description = "Operaciones CRUD para la gestion de generos literarios")
public class GeneroController {

    private final GeneroService generoService;

    @PostMapping
    @Operation(summary = "Crear un genero", description = "Registra un nuevo genero literario")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Genero creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneroDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, faltantes o nombre de genero ya registrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<GeneroDTO> crearGenero(
            @Parameter(description = "Datos del genero a crear", required = true)
            @Valid @RequestBody GeneroDTO generoDTO) {
        log.info("POST /api/v1/generos - nombre: {}", generoDTO.getNombre());
        GeneroDTO creado = generoService.crearGenero(generoDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar genero por ID", description = "Obtiene la informacion de un genero especifico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Genero encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneroDTO.class))),
            @ApiResponse(responseCode = "404", description = "Genero no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<GeneroDTO> obtenerPorId(
            @Parameter(description = "ID del genero", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/generos/{}", id);
        return ResponseEntity.ok(generoService.obtenerGeneroPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todos los generos", description = "Obtiene la lista completa de generos registrados")
    @ApiResponse(responseCode = "200", description = "Lista de generos obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = GeneroDTO.class))))
    public ResponseEntity<List<GeneroDTO>> obtenerTodos() {
        log.debug("GET /api/v1/generos");
        return ResponseEntity.ok(generoService.obtenerTodosLosGeneros());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar genero", description = "Actualiza los datos de un genero existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Genero actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneroDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o nombre de genero ya registrado por otro genero",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Genero no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<GeneroDTO> actualizar(
            @Parameter(description = "ID del genero a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos del genero", required = true)
            @Valid @RequestBody GeneroDTO generoDTO) {
        log.info("PUT /api/v1/generos/{}", id);
        return ResponseEntity.ok(generoService.actualizarGenero(id, generoDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar genero", description = "Elimina un genero del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Genero eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Genero no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionado con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del genero a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/generos/{}", id);
        generoService.eliminarGenero(id);
        return ResponseEntity.noContent().build();
    }
}
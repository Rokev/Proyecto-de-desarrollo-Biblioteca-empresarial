package presentationLayer.controller;

import bussinesLayer.dto.AutorDTO;
import bussinesLayer.service.AutorService;
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
@RequestMapping("/api/v1/autores")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autores", description = "Operaciones CRUD para la gestion de autores del catalogo")
public class AutorController {

    private final AutorService autorService;

    @PostMapping
    @Operation(summary = "Crear un autor", description = "Registra un nuevo autor en el catalogo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Autor creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AutorDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, faltantes o nombre de autor ya registrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<AutorDTO> crearAutor(
            @Parameter(description = "Datos del autor a crear", required = true)
            @Valid @RequestBody AutorDTO autorDTO) {
        log.info("POST /api/v1/autores - nombre: {}", autorDTO.getNombre());
        AutorDTO creado = autorService.crearAutor(autorDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar autor por ID", description = "Obtiene la informacion de un autor especifico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autor encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AutorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Autor no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<AutorDTO> obtenerPorId(
            @Parameter(description = "ID del autor", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/autores/{}", id);
        return ResponseEntity.ok(autorService.obtenerAutorPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todos los autores", description = "Obtiene la lista completa de autores registrados")
    @ApiResponse(responseCode = "200", description = "Lista de autores obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AutorDTO.class))))
    public ResponseEntity<List<AutorDTO>> obtenerTodos() {
        log.debug("GET /api/v1/autores");
        return ResponseEntity.ok(autorService.obtenerTodosLosAutores());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar autor", description = "Actualiza los datos de un autor existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autor actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AutorDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o nombre de autor ya registrado por otro autor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Autor no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<AutorDTO> actualizar(
            @Parameter(description = "ID del autor a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos del autor", required = true)
            @Valid @RequestBody AutorDTO autorDTO) {
        log.info("PUT /api/v1/autores/{}", id);
        return ResponseEntity.ok(autorService.actualizarAutor(id, autorDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar autor", description = "Elimina un autor del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Autor eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Autor no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionado con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del autor a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/autores/{}", id);
        autorService.eliminarAutor(id);
        return ResponseEntity.noContent().build();
    }
}
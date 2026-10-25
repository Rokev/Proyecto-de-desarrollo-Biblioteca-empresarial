package presentationLayer.controller;
import org.springframework.web.bind.annotation.RequestParam;

import bussinesLayer.dto.LibroDTO;
import bussinesLayer.service.LibroService;
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
@RequestMapping("/api/v1/libros")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Libros", description = "Operaciones CRUD para la gestion de libros del catalogo")
public class LibroController {

    private final LibroService libroService;

    @PostMapping
    @Operation(summary = "Crear un libro", description = "Registra un nuevo libro en el catalogo con su genero y autores")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Libro creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LibroDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, faltantes, ISBN ya registrado o anio de publicacion futuro",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Genero o autor no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<LibroDTO> crearLibro(
            @Parameter(description = "Datos del libro a crear", required = true)
            @Valid @RequestBody LibroDTO libroDTO) {
        log.info("POST /api/v1/libros - isbn: {}", libroDTO.getIsbn());
        LibroDTO creado = libroService.crearLibro(libroDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar libro por ID", description = "Obtiene la informacion de un libro especifico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Libro encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LibroDTO.class))),
            @ApiResponse(responseCode = "404", description = "Libro no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<LibroDTO> obtenerPorId(
            @Parameter(description = "ID del libro", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/libros/{}", id);
        return ResponseEntity.ok(libroService.obtenerLibroPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todos los libros", description = "Obtiene la lista completa de libros registrados")
    @ApiResponse(responseCode = "200", description = "Lista de libros obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = LibroDTO.class))))
    public ResponseEntity<List<LibroDTO>> obtenerTodos() {
        log.debug("GET /api/v1/libros");
        return ResponseEntity.ok(libroService.obtenerTodosLosLibros());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar libro", description = "Actualiza los datos de un libro existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Libro actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LibroDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, ISBN ya registrado por otro libro o anio de publicacion futuro",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Libro, genero o autor no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<LibroDTO> actualizar(
            @Parameter(description = "ID del libro a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos del libro", required = true)
            @Valid @RequestBody LibroDTO libroDTO) {
        log.info("PUT /api/v1/libros/{}", id);
        return ResponseEntity.ok(libroService.actualizarLibro(id, libroDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar libro", description = "Elimina un libro del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Libro eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Libro no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionado con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del libro a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/libros/{}", id);
        libroService.eliminarLibro(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar")
    @Operation(summary = "Buscar libros", description = "Busca libros con o sin filtros (titulo, autor, idioma, anio, genero) y, opcionalmente, por disponibilidad (en una sede)")
    @ApiResponse(responseCode = "200", description = "Consulta realizada exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = LibroDTO.class))))
    public ResponseEntity<List<LibroDTO>> buscarLibros(
            @Parameter(description = "Titulo (coincidencia parcial)") @RequestParam(required = false) String titulo,
            @Parameter(description = "Nombre del autor (coincidencia parcial)") @RequestParam(required = false) String autor,
            @Parameter(description = "Idioma") @RequestParam(required = false) String idioma,
            @Parameter(description = "Anio de publicacion") @RequestParam(required = false) Integer anio,
            @Parameter(description = "ID del genero") @RequestParam(required = false) Long generoId,
            @Parameter(description = "true: solo con ejemplares disponibles; false: solo sin ejemplares disponibles") @RequestParam(required = false) Boolean disponible,
            @Parameter(description = "Limita la disponibilidad a esta sede") @RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(libroService.buscarLibros(titulo, autor, idioma, anio, generoId, disponible, sedeId));
    }
}

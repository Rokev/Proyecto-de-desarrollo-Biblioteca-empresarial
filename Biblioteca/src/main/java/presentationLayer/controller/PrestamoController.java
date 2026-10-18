package presentationLayer.controller;

import bussinesLayer.dto.PrestamoDTO;
import bussinesLayer.service.PrestamoService;
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
@RequestMapping("/api/v1/prestamos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Prestamos", description = "Operaciones CRUD para la gestion de prestamos de ejemplares")
public class PrestamoController {

    private final PrestamoService prestamoService;

    @PostMapping
    @Operation(summary = "Crear un prestamo", description = "Registra un nuevo prestamo de un ejemplar a un usuario (modalidad SEDE, DOMICILIO o INTERCIUDAD)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Prestamo creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PrestamoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, usuario con bloqueo o maximo de prestamos vigentes, ejemplar no disponible, modalidad invalida, falta direccion de entrega o fecha de vencimiento anterior a la solicitud",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario, ejemplar o sede de retiro no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PrestamoDTO> crearPrestamo(
            @Parameter(description = "Datos del prestamo a crear", required = true)
            @Valid @RequestBody PrestamoDTO prestamoDTO) {
        log.info("POST /api/v1/prestamos - usuarioId: {}, ejemplarId: {}", prestamoDTO.getUsuarioId(), prestamoDTO.getEjemplarId());
        PrestamoDTO creado = prestamoService.crearPrestamo(prestamoDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar prestamo por ID", description = "Obtiene la informacion de un prestamo especifico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prestamo encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PrestamoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Prestamo no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PrestamoDTO> obtenerPorId(
            @Parameter(description = "ID del prestamo", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/prestamos/{}", id);
        return ResponseEntity.ok(prestamoService.obtenerPrestamoPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todos los prestamos", description = "Obtiene la lista completa de prestamos registrados")
    @ApiResponse(responseCode = "200", description = "Lista de prestamos obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PrestamoDTO.class))))
    public ResponseEntity<List<PrestamoDTO>> obtenerTodos() {
        log.debug("GET /api/v1/prestamos");
        return ResponseEntity.ok(prestamoService.obtenerTodosLosPrestamos());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar prestamo", description = "Actualiza los datos de un prestamo existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prestamo actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PrestamoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, intento de cambiar usuario o ejemplar, modalidad invalida o fechas inconsistentes",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Prestamo o sede de retiro no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PrestamoDTO> actualizar(
            @Parameter(description = "ID del prestamo a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos del prestamo", required = true)
            @Valid @RequestBody PrestamoDTO prestamoDTO) {
        log.info("PUT /api/v1/prestamos/{}", id);
        return ResponseEntity.ok(prestamoService.actualizarPrestamo(id, prestamoDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar prestamo", description = "Elimina un prestamo del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Prestamo eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Prestamo no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionado con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del prestamo a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/prestamos/{}", id);
        prestamoService.eliminarPrestamo(id);
        return ResponseEntity.noContent().build();
    }
}
package presentationLayer.controller;

import bussinesLayer.dto.MetodoPagoDTO;
import bussinesLayer.service.MetodoPagoService;
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
@RequestMapping("/api/v1/metodos-pago")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Metodos de pago", description = "Operaciones CRUD para la gestion de metodos de pago de los usuarios")
public class MetodoPagoController {

    private final MetodoPagoService metodoPagoService;

    @PostMapping
    @Operation(summary = "Crear un metodo de pago", description = "Registra un nuevo metodo de pago asociado a un usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Metodo de pago creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MetodoPagoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o faltantes",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<MetodoPagoDTO> crearMetodoPago(
            @Parameter(description = "Datos del metodo de pago a crear", required = true)
            @Valid @RequestBody MetodoPagoDTO metodoPagoDTO) {
        log.info("POST /api/v1/metodos-pago - usuarioId: {}", metodoPagoDTO.getUsuarioId());
        MetodoPagoDTO creado = metodoPagoService.crearMetodoPago(metodoPagoDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar metodo de pago por ID", description = "Obtiene la informacion de un metodo de pago especifico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metodo de pago encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MetodoPagoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Metodo de pago no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<MetodoPagoDTO> obtenerPorId(
            @Parameter(description = "ID del metodo de pago", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/metodos-pago/{}", id);
        return ResponseEntity.ok(metodoPagoService.obtenerMetodoPagoPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todos los metodos de pago", description = "Obtiene la lista completa de metodos de pago registrados")
    @ApiResponse(responseCode = "200", description = "Lista de metodos de pago obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = MetodoPagoDTO.class))))
    public ResponseEntity<List<MetodoPagoDTO>> obtenerTodos() {
        log.debug("GET /api/v1/metodos-pago");
        return ResponseEntity.ok(metodoPagoService.obtenerTodosLosMetodosPago());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar metodo de pago", description = "Actualiza los datos de un metodo de pago existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metodo de pago actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MetodoPagoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, intento de cambiar el usuario propietario o de dejar al usuario sin metodo predeterminado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Metodo de pago no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<MetodoPagoDTO> actualizar(
            @Parameter(description = "ID del metodo de pago a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos del metodo de pago", required = true)
            @Valid @RequestBody MetodoPagoDTO metodoPagoDTO) {
        log.info("PUT /api/v1/metodos-pago/{}", id);
        return ResponseEntity.ok(metodoPagoService.actualizarMetodoPago(id, metodoPagoDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar metodo de pago", description = "Elimina un metodo de pago del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Metodo de pago eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Metodo de pago no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionado con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del metodo de pago a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/metodos-pago/{}", id);
        metodoPagoService.eliminarMetodoPago(id);
        return ResponseEntity.noContent().build();
    }
}
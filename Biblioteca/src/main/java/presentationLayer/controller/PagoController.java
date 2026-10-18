package presentationLayer.controller;

import bussinesLayer.dto.PagoDTO;
import bussinesLayer.service.PagoService;
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
@RequestMapping("/api/v1/pagos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pagos", description = "Operaciones CRUD para la gestion de pagos de multas, suscripciones y reservas de sala")
public class PagoController {

    private final PagoService pagoService;

    @PostMapping
    @Operation(summary = "Crear un pago", description = "Registra un nuevo pago asociado a un usuario, un metodo de pago y su concepto (MULTA, SUSCRIPCION o RESERVA_SALA)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pago creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, referencia duplicada, concepto invalido, metodo de pago ajeno al usuario o falta el id del concepto",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario, multa, suscripcion o reserva de sala no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PagoDTO> crearPago(
            @Parameter(description = "Datos del pago a crear", required = true)
            @Valid @RequestBody PagoDTO pagoDTO) {
        log.info("POST /api/v1/pagos - referencia: {}", pagoDTO.getReferencia());
        PagoDTO creado = pagoService.crearPago(pagoDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar pago por ID", description = "Obtiene la informacion de un pago especifico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pago encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PagoDTO> obtenerPorId(
            @Parameter(description = "ID del pago", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/pagos/{}", id);
        return ResponseEntity.ok(pagoService.obtenerPagoPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todos los pagos", description = "Obtiene la lista completa de pagos registrados")
    @ApiResponse(responseCode = "200", description = "Lista de pagos obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PagoDTO.class))))
    public ResponseEntity<List<PagoDTO>> obtenerTodos() {
        log.debug("GET /api/v1/pagos");
        return ResponseEntity.ok(pagoService.obtenerTodosLosPagos());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar pago", description = "Actualiza los datos de un pago existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pago actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, referencia ya registrada por otro pago, concepto invalido o metodo de pago ajeno al usuario",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pago, usuario, multa, suscripcion o reserva de sala no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PagoDTO> actualizar(
            @Parameter(description = "ID del pago a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos del pago", required = true)
            @Valid @RequestBody PagoDTO pagoDTO) {
        log.info("PUT /api/v1/pagos/{}", id);
        return ResponseEntity.ok(pagoService.actualizarPago(id, pagoDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar pago", description = "Elimina un pago del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pago eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionado con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del pago a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/pagos/{}", id);
        pagoService.eliminarPago(id);
        return ResponseEntity.noContent().build();
    }
}
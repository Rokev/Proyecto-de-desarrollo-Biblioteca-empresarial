package presentationLayer.controller;

import bussinesLayer.dto.ComprobantePagoDTO;
import bussinesLayer.dto.PagoDTO;
import bussinesLayer.dto.SolicitudPagoDTO;
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
import org.springframework.web.bind.annotation.*;
import presentationLayer.advice.ApiError;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pagos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pagos y Pasarela Simulada", description = "Gestión de transacciones de pago, pasarela de tarjetas de prueba y consulta de comprobantes (RF-14, RF-15)")
public class PagoController {

    private final PagoService pagoService;

    // =========================================================================
    // PASARELA DE PAGO SIMULADO 
    // =========================================================================

    @PostMapping("/procesar")
    @Operation(summary = "Procesar pago con tarjeta simulada",
            description = "Simula el cobro mediante tarjeta de crédito/débito de prueba. No almacena datos sensibles. Activa suscripciones, libera multas o confirma envíos si la pasarela responde APROBADO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transacción procesada correctamente (resultado APROBADO o RECHAZADO)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ComprobantePagoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos, concepto no soportado o tarjeta mal formateada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario, multa o recurso asociado no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ComprobantePagoDTO> procesarPagoSimulado(
            @Parameter(description = "Datos de la transacción con tarjeta simulada", required = true)
            @Valid @RequestBody SolicitudPagoDTO solicitud) {
        log.info("POST /api/v1/pagos/procesar - Usuario: {}, Concepto: {}", solicitud.getUsuarioId(), solicitud.getConcepto());
        return ResponseEntity.ok(pagoService.procesarPagoSimulado(solicitud));
    }

    @GetMapping("/comprobante/{id}")
    @Operation(summary = "Consultar comprobante de pago por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comprobante de pago encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ComprobantePagoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Registro de pago no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ComprobantePagoDTO> obtenerComprobantePorId(
            @Parameter(description = "ID único del pago", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(pagoService.obtenerComprobantePorId(id));
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = "Consultar historial de pagos de un usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial de transacciones de pago del usuario",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ComprobantePagoDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<ComprobantePagoDTO>> obtenerPagosPorUsuario(
            @Parameter(description = "ID del usuario", required = true, example = "1") @PathVariable Long usuarioId) {
        return ResponseEntity.ok(pagoService.obtenerPagosPorUsuario(usuarioId));
    }

    // =========================================================================
    // MÉTODOS CRUD TRADICIONALES
    // =========================================================================

    @PostMapping
    @Operation(summary = "Crear registro directo de pago (CRUD Administrador)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pago creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Referencia duplicada o datos de pago inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario, método de pago o recurso asociado no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PagoDTO> crearPago(
            @Parameter(description = "Datos del pago a registrar", required = true)
            @Valid @RequestBody PagoDTO pagoDTO) {
        log.info("POST /api/v1/pagos - Referencia: {}", pagoDTO.getReferencia());
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoService.crearPago(pagoDTO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de un pago por ID (CRUD)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pago encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PagoDTO> obtenerPagoPorId(
            @Parameter(description = "ID único del pago", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(pagoService.obtenerPagoPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todos los pagos del sistema (CRUD Administrador)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado completo de registros de pago",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PagoDTO.class))))
    })
    public ResponseEntity<List<PagoDTO>> obtenerTodosLosPagos() {
        return ResponseEntity.ok(pagoService.obtenerTodosLosPagos());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar registro de pago (CRUD Administrador)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pago actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Referencia en uso por otro registro o datos inconsistentes",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PagoDTO> actualizarPago(
            @Parameter(description = "ID único del pago a actualizar", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "Nuevos datos del pago", required = true) @Valid @RequestBody PagoDTO pagoDTO) {
        log.info("PUT /api/v1/pagos/{} - Referencia: {}", id, pagoDTO.getReferencia());
        return ResponseEntity.ok(pagoService.actualizarPago(id, pagoDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar registro de pago (CRUD Administrador)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pago eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminarPago(
            @Parameter(description = "ID único del pago a eliminar", required = true, example = "1") @PathVariable Long id) {
        log.info("DELETE /api/v1/pagos/{}", id);
        pagoService.eliminarPago(id);
        return ResponseEntity.noContent().build();
    }
}
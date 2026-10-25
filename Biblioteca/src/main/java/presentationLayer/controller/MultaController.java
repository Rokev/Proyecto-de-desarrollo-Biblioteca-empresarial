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
import org.springframework.web.bind.annotation.*;
import presentationLayer.advice.ApiError;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/multas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Multas", description = "Operaciones para la gestión de multas, recargos y saldos de los usuarios")
public class MultaController {

    private final MultaService multaService;

    @PostMapping
    @Operation(summary = "Crear una multa", description = "Registra una nueva multa asociada a un usuario y a un préstamo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Multa creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MultaDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o faltantes",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario o préstamo no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<MultaDTO> crearMulta(
            @Parameter(description = "Datos de la multa a crear", required = true)
            @Valid @RequestBody MultaDTO multaDTO) {
        log.info("POST /api/v1/multas - usuarioId: {}", multaDTO.getUsuarioId());
        MultaDTO creada = multaService.crearMulta(multaDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = " Consultar multas de un usuario", description = "Obtiene las multas del usuario, opcionalmente filtrando por estado (PENDIENTE, PAGADA)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de multas del usuario",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = MultaDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<MultaDTO>> obtenerPorUsuario(
            @Parameter(description = "ID del usuario", required = true, example = "1") @PathVariable Long usuarioId,
            @Parameter(description = "Estado opcional (PENDIENTE, PAGADA)", example = "PENDIENTE") @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(multaService.obtenerMultasPorUsuario(usuarioId, estado));
    }

    @GetMapping("/usuario/{usuarioId}/saldo-pendiente")
    @Operation(summary = "Consultar saldo total pendiente de pago de un usuario", description = "Suma el valor total de todas las multas y recargos en estado PENDIENTE")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Monto total pendiente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<BigDecimal> obtenerSaldoPendiente(
            @Parameter(description = "ID del usuario", required = true, example = "1") @PathVariable Long usuarioId) {
        return ResponseEntity.ok(multaService.obtenerSaldoPendienteUsuario(usuarioId));
    }

    @PostMapping("/danio-perdida")
    @Operation(summary = "Registrar multa por libro dañado o perdido", description = "El bibliotecario aplica un cargo económico al usuario por daño o pérdida de un ejemplar")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Multa por daño/pérdida creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MultaDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o monto <= 0",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario o préstamo no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<MultaDTO> generarMultaDanioPerdida(
            @Parameter(description = "ID del usuario", required = true, example = "1") @RequestParam Long usuarioId,
            @Parameter(description = "ID del préstamo asociado (opcional)", example = "1") @RequestParam(required = false) Long prestamoId,
            @Parameter(description = "DANADO o PERDIDO", required = true, example = "DANADO") @RequestParam String tipo,
            @Parameter(description = "Monto a cobrar (opcional: si se omite se usa el monto configurado,  75)", example = "45000.00") @RequestParam(required = false) BigDecimal monto,
            @Parameter(description = "Motivo detallado", required = true, example = "Libro devuelto con la portada destruida") @RequestParam String motivo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(multaService.generarMultaPorDanioOPerdida(usuarioId, prestamoId, tipo, monto, motivo));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar multa por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Multa encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MultaDTO.class))),
            @ApiResponse(responseCode = "404", description = "Multa no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<MultaDTO> obtenerPorId(
            @Parameter(description = "ID de la multa", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(multaService.obtenerMultaPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todas las multas")
    @ApiResponse(responseCode = "200", description = "Lista completa de multas",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = MultaDTO.class))))
    public ResponseEntity<List<MultaDTO>> obtenerTodos() {
        return ResponseEntity.ok(multaService.obtenerTodasLasMultas());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar multa")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Multa actualizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MultaDTO.class))),
            @ApiResponse(responseCode = "404", description = "Multa no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<MultaDTO> actualizar(
            @Parameter(description = "ID de la multa a actualizar", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "Nuevos datos de la multa", required = true) @Valid @RequestBody MultaDTO multaDTO) {
        return ResponseEntity.ok(multaService.actualizarMulta(id, multaDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar multa")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Multa eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Multa no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID de la multa a eliminar", required = true, example = "1") @PathVariable Long id) {
        multaService.eliminarMulta(id);
        return ResponseEntity.noContent().build();
    }
}

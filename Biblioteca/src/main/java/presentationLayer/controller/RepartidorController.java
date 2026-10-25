package presentationLayer.controller;

import bussinesLayer.dto.PedidoInterciudadDTO;
import bussinesLayer.service.PedidoInterciudadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import presentationLayer.advice.ApiError;

import java.util.List;

@RestController
@RequestMapping("/api/v1/repartidores")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Módulo de Repartidor", description = "Operaciones exclusivas para el rol Repartidor")
public class RepartidorController {

    private final PedidoInterciudadService pedidoService;

    @GetMapping("/{repartidorId}/asignaciones")
    @Operation(summary = "Visualizar entregas a domicilio y traslados asignados",
            description = "Muestra las entregas a domicilio asignadas al repartidor (EN_TRANSITO) y los traslados pendientes de retorno (DEVUELTO_PENDIENTE_TRASLADO, EN_CAMINO_SEDE_ORIGEN).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de tareas asignadas al repartidor",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PedidoInterciudadDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Repartidor no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<PedidoInterciudadDTO>> obtenerAsignaciones(
            @Parameter(description = "ID del repartidor", required = true, example = "5") 
            @PathVariable Long repartidorId) {
        log.info("GET /api/v1/repartidores/{}/asignaciones", repartidorId);
        return ResponseEntity.ok(pedidoService.obtenerAsignacionesRepartidor(repartidorId));
    }

    @PostMapping("/{repartidorId}/pedidos/{pedidoId}/confirmar-entrega")
    @Operation(summary = "Confirmar entrega de un libro a domicilio",
            description = "Cambia el estado del pedido a ENTREGADO, inicia el plazo de devolución de 14 días y notifica al usuario la fecha límite y sede de devolución.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrega a domicilio confirmada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoInterciudadDTO.class))),
            @ApiResponse(responseCode = "400", description = "El pedido no está en estado EN_TRANSITO",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pedido o repartidor no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PedidoInterciudadDTO> confirmarEntregaDomicilio(
            @Parameter(description = "ID del repartidor", required = true, example = "5") @PathVariable Long repartidorId,
            @Parameter(description = "ID del pedido interciudad", required = true, example = "1") @PathVariable Long pedidoId) {
        log.info("POST /api/v1/repartidores/{}/pedidos/{}/confirmar-entrega", repartidorId, pedidoId);
        return ResponseEntity.ok(pedidoService.confirmarEntregaDomicilio(pedidoId, repartidorId));
    }

    @PostMapping("/{repartidorId}/pedidos/{pedidoId}/no-recibido")
    @Operation(summary = "Registrar que el usuario no recibió el pedido",
            description = "Si el usuario no recibe el libro en la fecha/hora indicadas, cambia el estado a NO_RECIBIDO, aplica el recargo fijo ($15,000) y notifica el traslado a la sede de devolución.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Intento fallido registrado y recargo aplicado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoInterciudadDTO.class))),
            @ApiResponse(responseCode = "400", description = "El pedido no está en estado EN_TRANSITO",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pedido o repartidor no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PedidoInterciudadDTO> registrarUsuarioNoRecibio(
            @Parameter(description = "ID del repartidor", required = true, example = "5") @PathVariable Long repartidorId,
            @Parameter(description = "ID del pedido interciudad", required = true, example = "1") @PathVariable Long pedidoId) {
        log.info("POST /api/v1/repartidores/{}/pedidos/{}/no-recibido", repartidorId, pedidoId);
        return ResponseEntity.ok(pedidoService.registrarUsuarioNoRecibio(pedidoId, repartidorId));
    }

    @PostMapping("/{repartidorId}/pedidos/{pedidoId}/iniciar-traslado")
    @Operation(summary = "Recoger libro devuelto en sede de devolución e iniciar retorno a sede de origen",
            description = "Cambia el estado de DEVUELTO_PENDIENTE_TRASLADO a EN_CAMINO_SEDE_ORIGEN, asigna al repartidor como responsable y notifica a la sede de origen.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Traslado de retorno iniciado correctamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoInterciudadDTO.class))),
            @ApiResponse(responseCode = "400", description = "El pedido no está en estado DEVUELTO_PENDIENTE_TRASLADO",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pedido o repartidor no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PedidoInterciudadDTO> iniciarTrasladoRetorno(
            @Parameter(description = "ID del repartidor", required = true, example = "5") @PathVariable Long repartidorId,
            @Parameter(description = "ID del pedido interciudad", required = true, example = "1") @PathVariable Long pedidoId) {
        log.info("POST /api/v1/repartidores/{}/pedidos/{}/iniciar-traslado", repartidorId, pedidoId);
        return ResponseEntity.ok(pedidoService.iniciarTrasladoRetorno(pedidoId, repartidorId));
    }
}
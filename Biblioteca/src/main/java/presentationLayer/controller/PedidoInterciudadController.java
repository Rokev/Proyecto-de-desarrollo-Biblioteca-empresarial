package presentationLayer.controller;

import bussinesLayer.dto.PedidoInterciudadDTO;
import bussinesLayer.dto.SolicitudPedidoInterciudadDTO;
import bussinesLayer.service.PedidoInterciudadService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import presentationLayer.advice.ApiError;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pedidos-interciudad")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pedidos interciudad", description = "Pedidos de libros de otra ciudad con entrega a domicilio para usuarios con suscripcion")
public class PedidoInterciudadController {

    private final PedidoInterciudadService pedidoService;

    @PostMapping
    @Operation(summary = "Solicitar un libro de otra ciudad (RF-26)",
            description = "Requiere suscripcion vigente. Reserva un ejemplar disponible en una ciudad distinta a la de entrega y crea el pedido y su prestamo vinculado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido creado y confirmado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoInterciudadDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, sin suscripcion vigente, bloqueo de prestamos activo o sin ejemplares disponibles en otras ciudades",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario o libro no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PedidoInterciudadDTO> solicitarPedido(
            @Parameter(description = "Datos del pedido", required = true)
            @Valid @RequestBody SolicitudPedidoInterciudadDTO solicitud) {
        log.info("POST /api/v1/pedidos-interciudad - usuarioId: {}, libroId: {}", solicitud.getUsuarioId(), solicitud.getLibroId());
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.solicitarPedido(solicitud));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar pedido interciudad por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoInterciudadDTO.class))),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PedidoInterciudadDTO> obtenerPorId(
            @Parameter(description = "ID del pedido", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtenerPedidoPorId(id));
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = "Consultar el estado de mis pedidos interciudad",
            description = "Lista los pedidos del usuario, del mas reciente al mas antiguo. Estados: SOLICITADO, EN_TRANSITO, DISPONIBLE_PARA_RETIRO, RETIRADO, CANCELADO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de pedidos del usuario",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PedidoInterciudadDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Estado invalido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<PedidoInterciudadDTO>> obtenerDeUsuario(
            @Parameter(description = "ID del usuario", required = true, example = "1") @PathVariable Long usuarioId,
            @Parameter(description = "Filtrar por estado (opcional)", example = "EN_TRANSITO") @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(pedidoService.obtenerPedidosDeUsuario(usuarioId, estado));
    }

    @GetMapping("/estado/{estado}")
    @Operation(summary = "Listar pedidos por estado (bibliotecario)",
            description = "Para que el bibliotecario vea los pedidos pendientes de procesar, los mas antiguos primero.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de pedidos en ese estado",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PedidoInterciudadDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Estado invalido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<PedidoInterciudadDTO>> obtenerPorEstado(
            @Parameter(description = "Estado del pedido", required = true, example = "SOLICITADO") @PathVariable String estado) {
        return ResponseEntity.ok(pedidoService.obtenerPedidosPorEstado(estado));
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar un pedido interciudad",
            description = "Solo si el pedido esta SOLICITADO. Si esta EN_TRANSITO se bloquea la cancelacion y se notifica al usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido cancelado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoInterciudadDTO.class))),
            @ApiResponse(responseCode = "400", description = "El pedido no pertenece al usuario, esta en transito o su estado no permite cancelar",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PedidoInterciudadDTO> cancelar(
            @Parameter(description = "ID del pedido", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "ID del usuario dueño del pedido", required = true, example = "1") @RequestParam Long usuarioId) {
        log.info("POST /api/v1/pedidos-interciudad/{}/cancelar - usuarioId: {}", id, usuarioId);
        return ResponseEntity.ok(pedidoService.cancelarPedido(id, usuarioId));
    }

    @PostMapping("/{id}/en-transito")
    @Operation(summary = "Marcar pedido en transito (bibliotecario)", description = "SOLICITADO -> EN_TRANSITO. Notifica al usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido en transito",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoInterciudadDTO.class))),
            @ApiResponse(responseCode = "400", description = "El pedido no esta SOLICITADO",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pedido o bibliotecario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PedidoInterciudadDTO> marcarEnTransito(
            @Parameter(description = "ID del pedido", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "ID del bibliotecario", required = true, example = "2") @RequestParam Long bibliotecarioId) {
        return ResponseEntity.ok(pedidoService.marcarEnTransito(id, bibliotecarioId));
    }

    @PostMapping("/{id}/disponible-para-retiro")
    @Operation(summary = "Marcar pedido disponible para retiro (bibliotecario)", description = "EN_TRANSITO -> DISPONIBLE_PARA_RETIRO. Notifica al usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido disponible",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoInterciudadDTO.class))),
            @ApiResponse(responseCode = "400", description = "El pedido no esta EN_TRANSITO",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pedido o bibliotecario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PedidoInterciudadDTO> marcarDisponible(
            @Parameter(description = "ID del pedido", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "ID del bibliotecario", required = true, example = "2") @RequestParam Long bibliotecarioId) {
        return ResponseEntity.ok(pedidoService.marcarDisponibleParaRetiro(id, bibliotecarioId));
    }

    @PostMapping("/{id}/retirado")
    @Operation(summary = "Registrar entrega del pedido (bibliotecario)",
            description = "DISPONIBLE_PARA_RETIRO -> RETIRADO. Activa el prestamo y empieza a correr el plazo de devolucion.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrega registrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoInterciudadDTO.class))),
            @ApiResponse(responseCode = "400", description = "El pedido no esta DISPONIBLE_PARA_RETIRO",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pedido o bibliotecario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PedidoInterciudadDTO> marcarRetirado(
            @Parameter(description = "ID del pedido", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "ID del bibliotecario", required = true, example = "2") @RequestParam Long bibliotecarioId) {
        return ResponseEntity.ok(pedidoService.marcarRetirado(id, bibliotecarioId));
    }
}
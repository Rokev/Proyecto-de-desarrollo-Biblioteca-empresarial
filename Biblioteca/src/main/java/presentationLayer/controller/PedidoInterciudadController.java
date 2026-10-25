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
import org.springframework.web.bind.annotation.*;
import presentationLayer.advice.ApiError;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pedidos-interciudad")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pedidos Interciudad", description = "Gestión de pedidos interciudad para Suscriptores, Bibliotecarios y Repartidores")
public class PedidoInterciudadController {

    private final PedidoInterciudadService pedidoService;

    // =========================================================================
    // MÓDULO: USUARIO CON SUSCRIPCIÓN
    // =========================================================================

    @PostMapping
    @Operation(summary = "Solicitar pedido interciudad",
            description = "Requiere suscripción vigente. Solicita un libro indicando dirección de entrega, sede de devolución elegida y fecha/hora de recepción.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido interciudad creado y confirmado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoInterciudadDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, usuario sin suscripción activa, sanciones/bloqueos vigentes o fecha de recepción superior a 7 días",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario, libro o sede de devolución no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PedidoInterciudadDTO> solicitarPedido(
            @Parameter(description = "Datos de la solicitud de pedido interciudad", required = true)
            @Valid @RequestBody SolicitudPedidoInterciudadDTO solicitud) {
        log.info("POST /api/v1/pedidos-interciudad - usuarioId: {}, libroId: {}", solicitud.getUsuarioId(), solicitud.getLibroId());
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.solicitarPedido(solicitud));
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar un pedido interciudad",
            description = "Permite cancelar únicamente si el pedido está en estado PENDIENTE_PAGO o SOLICITADO. Si ya fue despachado o está en tránsito se bloquea la cancelación.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido cancelado y reembolso registrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoInterciudadDTO.class))),
            @ApiResponse(responseCode = "400", description = "El pedido no pertenece al usuario o su estado actual no permite cancelación",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pedido interciudad no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PedidoInterciudadDTO> cancelarPedido(
            @Parameter(description = "ID único del pedido", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "ID del usuario propietario del pedido", required = true, example = "1") @RequestParam Long usuarioId) {
        log.info("POST /api/v1/pedidos-interciudad/{}/cancelar - usuarioId: {}", id, usuarioId);
        return ResponseEntity.ok(pedidoService.cancelarPedido(id, usuarioId));
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = "Consultar el estado de mis pedidos interciudad",
            description = "Retorna el historial de pedidos del usuario, filtrable opcionalmente por estado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de pedidos del usuario",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PedidoInterciudadDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Estado de filtro inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<PedidoInterciudadDTO>> obtenerDeUsuario(
            @Parameter(description = "ID del usuario", required = true, example = "1") @PathVariable Long usuarioId,
            @Parameter(description = "Filtro opcional por estado (ej: SOLICITADO, EN_TRANSITO, ENTREGADO)", example = "EN_TRANSITO") @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(pedidoService.obtenerPedidosDeUsuario(usuarioId, estado));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar pedido interciudad por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoInterciudadDTO.class))),
            @ApiResponse(responseCode = "404", description = "Pedido interciudad no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PedidoInterciudadDTO> obtenerPorId(
            @Parameter(description = "ID del pedido", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtenerPedidoPorId(id));
    }

    // =========================================================================
    // MÓDULO: BIBLIOTECARIO
    // =========================================================================

    @GetMapping("/sede-origen/{sedeId}")
    @Operation(summary = "Visualizar pedidos pendientes de salir desde la sede de origen",
            description = "Muestra al bibliotecario los pedidos solicitados que salen de su sede o que deben regresar a ella.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de pedidos pertenecientes a la sede de origen",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PedidoInterciudadDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Sede no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<PedidoInterciudadDTO>> obtenerPedidosSedeOrigen(
            @Parameter(description = "ID de la sede de origen", required = true, example = "1") @PathVariable Long sedeId) {
        return ResponseEntity.ok(pedidoService.obtenerPedidosSedeOrigen(sedeId));
    }

        @GetMapping("/sede-devolucion/{sedeId}")
        @Operation(summary = "Visualizar pedidos que se devuelven en la sede",
                description = "Muestra al bibliotecario los pedidos que deben devolverse, esperar recogida o trasladarse desde su sede de devolución.")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Listado de pedidos de la sede de devolución",
                        content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PedidoInterciudadDTO.class)))),
                @ApiResponse(responseCode = "404", description = "Sede no encontrada",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
        })
        public ResponseEntity<List<PedidoInterciudadDTO>> obtenerPedidosSedeDevolucion(
                @Parameter(description = "ID de la sede de devolución", required = true, example = "2") @PathVariable Long sedeId) {
            return ResponseEntity.ok(pedidoService.obtenerPedidosSedeDevolucion(sedeId));
        }

    @PostMapping("/{id}/despachar")
    @Operation(summary = "Registrar envío entregándolo al repartidor",
            description = "Cambia el estado del pedido de SOLICITADO a EN_TRANSITO, asigna el repartidor y descuenta el stock.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Envío registrado y asignado al repartidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoInterciudadDTO.class))),
            @ApiResponse(responseCode = "400", description = "El pedido no está en estado SOLICITADO",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pedido, bibliotecario o repartidor no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PedidoInterciudadDTO> despacharPedido(
            @Parameter(description = "ID del pedido", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "ID del bibliotecario que registra el despacho", required = true, example = "2") @RequestParam Long bibliotecarioId,
            @Parameter(description = "ID del repartidor asignado", required = true, example = "5") @RequestParam Long repartidorId) {
        return ResponseEntity.ok(pedidoService.despacharPedido(id, bibliotecarioId, repartidorId));
    }

    @PostMapping("/{id}/recepcion-no-recibido")
    @Operation(summary = "Registrar recepción en sede de devolución de un pedido no recibido",
            description = "Cambia el estado a EN_ESPERA_DE_RECOGIDA e inicia el plazo máximo para que el usuario retire el paquete.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recepción registrada en sede de devolución",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoInterciudadDTO.class))),
            @ApiResponse(responseCode = "400", description = "El pedido no está en estado NO_RECIBIDO",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pedido o bibliotecario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PedidoInterciudadDTO> registrarRecepcionNoRecibido(
            @Parameter(description = "ID del pedido", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "ID del bibliotecario de la sede de devolución", required = true, example = "2") @RequestParam Long bibliotecarioId) {
        return ResponseEntity.ok(pedidoService.registrarRecepcionNoRecibido(id, bibliotecarioId));
    }

    @PostMapping("/{id}/entrega-sede")
    @Operation(summary = "Registrar la entrega al usuario en la sede de devolución",
            description = "Entrega en mostrador un paquete no recibido previamente. Cambia el estado a ENTREGADO e inicia el plazo de préstamo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrega registrada en sede",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoInterciudadDTO.class))),
            @ApiResponse(responseCode = "400", description = "El pedido no está en estado EN_ESPERA_DE_RECOGIDA",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pedido o bibliotecario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PedidoInterciudadDTO> registrarEntregaEnSede(
            @Parameter(description = "ID del pedido", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "ID del bibliotecario", required = true, example = "2") @RequestParam Long bibliotecarioId) {
        return ResponseEntity.ok(pedidoService.registrarEntregaEnSede(id, bibliotecarioId));
    }

    @PostMapping("/{id}/devolucion-sede")
    @Operation(summary = "Registrar devolución del libro en la sede elegida",
            description = "Valida que la recepción sea en la sede de devolución seleccionada. Cambia estado a DEVUELTO_PENDIENTE_TRASLADO y cierra el préstamo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Devolución aceptada en sede",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoInterciudadDTO.class))),
            @ApiResponse(responseCode = "400", description = "La sede ingresada no coincide con la sede de devolución seleccionada en el pedido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pedido, bibliotecario o sede no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PedidoInterciudadDTO> registrarDevolucionEnSede(
            @Parameter(description = "ID del pedido", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "ID del bibliotecario receptor", required = true, example = "2") @RequestParam Long bibliotecarioId,
            @Parameter(description = "ID de la sede donde se realiza la devolución", required = true, example = "2") @RequestParam Long sedeId) {
        return ResponseEntity.ok(pedidoService.registrarDevolucionEnSede(id, bibliotecarioId, sedeId));
    }

    @PostMapping("/{id}/cerrar-origen")
    @Operation(summary = "Registrar recepción final en sede de origen",
            description = "Repone el stock de la sede de origen y pasa el pedido a estado CERRADO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido cerrado y stock repuesto",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoInterciudadDTO.class))),
            @ApiResponse(responseCode = "400", description = "El pedido no está en el flujo de retorno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pedido o bibliotecario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PedidoInterciudadDTO> registrarRecepcionEnSedeOrigen(
            @Parameter(description = "ID del pedido", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "ID del bibliotecario de la sede de origen", required = true, example = "2") @RequestParam Long bibliotecarioId) {
        return ResponseEntity.ok(pedidoService.registrarRecepcionEnSedeOrigen(id, bibliotecarioId));
    }

    // =========================================================================
    // MÓDULO: REPARTIDOR
    // =========================================================================

    @GetMapping("/repartidor/{repartidorId}")
    @Operation(summary = "Visualizar entregas y traslados asignados al repartidor",
            description = "Muestra únicamente las entregas a domicilio pendientes y los traslados asignados a ese repartidor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de asignaciones del repartidor",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PedidoInterciudadDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Repartidor no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<PedidoInterciudadDTO>> obtenerAsignacionesRepartidor(
            @Parameter(description = "ID del repartidor", required = true, example = "5") @PathVariable Long repartidorId) {
        return ResponseEntity.ok(pedidoService.obtenerAsignacionesRepartidor(repartidorId));
    }

    @PostMapping("/{id}/confirmar-entrega")
    @Operation(summary = "Confirmar entrega del libro a domicilio",
            description = "Cambia el estado a ENTREGADO, inicia el plazo de devolución y notifica la fecha límite al usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrega a domicilio confirmada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoInterciudadDTO.class))),
            @ApiResponse(responseCode = "400", description = "El pedido no está en estado EN_TRANSITO",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pedido o repartidor no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PedidoInterciudadDTO> confirmarEntregaDomicilio(
            @Parameter(description = "ID del pedido", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "ID del repartidor", required = true, example = "5") @RequestParam Long repartidorId) {
        return ResponseEntity.ok(pedidoService.confirmarEntregaDomicilio(id, repartidorId));
    }

    @PostMapping("/{id}/registrar-no-recibido")
    @Operation(summary = "Registrar que el usuario no recibió el pedido",
            description = "Aplica el recargo fijo configurado, cambia el estado a NO_RECIBIDO y notifica que el paquete se enviará a la sede de devolución.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro de intento fallido exitoso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoInterciudadDTO.class))),
            @ApiResponse(responseCode = "400", description = "El pedido no está en estado EN_TRANSITO",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pedido o repartidor no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PedidoInterciudadDTO> registrarUsuarioNoRecibio(
            @Parameter(description = "ID del pedido", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "ID del repartidor", required = true, example = "5") @RequestParam Long repartidorId) {
        return ResponseEntity.ok(pedidoService.registrarUsuarioNoRecibio(id, repartidorId));
    }

    @PostMapping("/{id}/iniciar-traslado-retorno")
    @Operation(summary = "Recoger libro devuelto en sede de devolución y llevarlo a sede de origen",
            description = "Pasa el estado de DEVUELTO_PENDIENTE_TRASLADO a EN_CAMINO_SEDE_ORIGEN asignando al repartidor como responsable.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Traslado de retorno iniciado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PedidoInterciudadDTO.class))),
            @ApiResponse(responseCode = "400", description = "El pedido no está pendiente de traslado de retorno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pedido o repartidor no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PedidoInterciudadDTO> iniciarTrasladoRetorno(
            @Parameter(description = "ID del pedido", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "ID del repartidor", required = true, example = "5") @RequestParam Long repartidorId) {
        return ResponseEntity.ok(pedidoService.iniciarTrasladoRetorno(id, repartidorId));
    }
}
package presentationLayer.controller;

import bussinesLayer.dto.ReservaSalaDTO;
import bussinesLayer.service.ReservaSalaService;
import bussinesLayer.service.SalaEstudioService;
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
import org.springframework.web.bind.annotation.RequestParam;

import presentationLayer.advice.ApiError;
import presentationLayer.security.AccesoUsuario;


import java.util.List;

@RestController
@RequestMapping("/api/v1/reservas-sala")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reservas de sala", description = "Operaciones CRUD para la gestion de reservas de salas de estudio")
public class ReservaSalaController {

        private final ReservaSalaService reservaSalaService;
        private final AccesoUsuario acceso;
        private final SalaEstudioService salaEstudioService;

    @PostMapping
    @Operation(summary = "Crear una reserva de sala", description = "Registra una nueva reserva de una sala de estudio para un usuario, el valor base y el descuento por suscripcion los calcula el servidor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva de sala creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReservaSalaDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, fecha de inicio en el pasado, fechas inconsistentes, sala inactiva o sala ya reservada en ese horario",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario o sala de estudio no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ReservaSalaDTO> crearReservaSala(
            @Parameter(description = "Datos de la reserva de sala a crear", required = true)
            @Valid @RequestBody ReservaSalaDTO reservaSalaDTO) {
        acceso.validarPropietario(reservaSalaDTO.getUsuarioId());
        if (!acceso.esPersonal()){
                reservaSalaDTO.setEstado(null); //el servicio la deja en PENDIENTE_PAGO
                reservaSalaDTO.setFechaCancelacion(null);
        }
        log.info("POST /api/v1/reservas-sala - usuarioId: {}, salaId: {}", reservaSalaDTO.getUsuarioId(), reservaSalaDTO.getSalaId());
        ReservaSalaDTO creada = reservaSalaService.crearReservaSala(reservaSalaDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar reserva de sala por ID", description = "Obtiene la informacion de una reserva de sala especifica")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva de sala encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReservaSalaDTO.class))),
            @ApiResponse(responseCode = "404", description = "Reserva de sala no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ReservaSalaDTO> obtenerPorId(
            @Parameter(description = "ID de la reserva de sala", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/reservas-sala/{}", id);
        var reserva = reservaSalaService.obtenerReservaSalaPorId(id);
        acceso.validarPropietarioOSede(reserva.getUsuarioId(), sedeDeLaReserva(reserva));
        return ResponseEntity.ok(reserva);
    }

    @GetMapping
    @Operation(summary = "Listar todas las reservas de sala", description = "Obtiene la lista completa de reservas de sala registradas")
    @ApiResponse(responseCode = "200", description = "Lista de reservas de sala obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReservaSalaDTO.class))))
    public ResponseEntity<List<ReservaSalaDTO>> obtenerTodos() {
        log.debug("GET /api/v1/reservas-sala");
        return ResponseEntity.ok(acceso.visibles(reservaSalaService.obtenerTodasLasReservasSala(),
        ReservaSalaDTO::getUsuarioId, r -> acceso.sedes(sedeDeLaReserva(r))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar reserva de sala", description = "Actualiza los datos de una reserva de sala existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva de sala actualizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReservaSalaDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o intento de cambiar la sala o el horario (cancele y cree una nueva reserva)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Reserva de sala, usuario o sala de estudio no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ReservaSalaDTO> actualizar(
            @Parameter(description = "ID de la reserva de sala a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos de la reserva de sala", required = true)
            @Valid @RequestBody ReservaSalaDTO reservaSalaDTO) {
        log.info("PUT /api/v1/reservas-sala/{}", id);
        acceso.validarSede(sedeDeLaReserva(reservaSalaService.obtenerReservaSalaPorId(id)));
        return ResponseEntity.ok(reservaSalaService.actualizarReservaSala(id, reservaSalaDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar reserva de sala", description = "Elimina una reserva de sala del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reserva de sala eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Reserva de sala no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionada con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID de la reserva de sala a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/reservas-sala/{}", id);
        acceso.validarSede(sedeDeLaReserva(reservaSalaService.obtenerReservaSalaPorId(id)));
        reservaSalaService.eliminarReservaSala(id);
        return ResponseEntity.noContent().build();
    }

        @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar reserva de sala", description = "Cancela una reserva PENDIENTE_PAGO o CONFIRMADA y libera la sala (RF-24)")
    public ResponseEntity<ReservaSalaDTO> cancelarReservaSala(
            @Parameter(description = "ID de la reserva", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "ID del usuario propietario de la reserva", required = true, example = "1") @RequestParam Long usuarioId) {
        var reserva = reservaSalaService.obtenerReservaSalaPorId(id);
        acceso.validarPropietarioOSede(usuarioId, sedeDeLaReserva(reserva));
        return ResponseEntity.ok(reservaSalaService.cancelarReservaSala(id, usuarioId));
    }

    private Long sedeDeLaReserva(ReservaSalaDTO reserva) {
        return salaEstudioService.obtenerSalaEstudioPorId(reserva.getSalaId()).getSedeId();
    }
}
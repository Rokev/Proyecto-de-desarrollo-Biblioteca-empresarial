package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Información detallada de un pedido interciudad de un libro con entrega a domicilio y sede de devolución elegida (usuarios con suscripción)")
public class PedidoInterciudadDTO {

    @Schema(description = "ID único del pedido", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Estado actual: SOLICITADO, EN_TRANSITO, ENTREGADO, NO_RECIBIDO, EN_ESPERA_DE_RECOGIDA, DEVUELTO_PENDIENTE_TRASLADO, EN_CAMINO_SEDE_ORIGEN, CANCELADO, CERRADO", example = "SOLICITADO")
    private String estado;

    @Schema(description = "Ciudad donde se entrega el libro", example = "Medellín")
    private String ciudadEntrega;

    @Schema(description = "Dirección exacta de entrega a domicilio", example = "Calle 10 # 20-30")
    private String direccionEntrega;

    @Schema(description = "Fecha en que el usuario indicó que puede recibir el pedido", example = "2026-10-28")
    private LocalDate fechaRecepcion;

    @Schema(description = "Hora o rango de hora en que el usuario puede recibir el paquete", example = "14:00 - 16:00")
    private String horaRecepcion;

    @Schema(description = "Costo del envío calculado según el par de ciudades", example = "12000.00")
    private Double costoEnvio;

    @Schema(description = "Fecha y hora en que se creó la solicitud", example = "2026-10-24T14:30:00")
    private LocalDateTime fechaSolicitud;

    @Schema(description = "Fecha y hora en que el bibliotecario despachó el pedido al repartidor", example = "2026-10-25T09:00:00")
    private LocalDateTime fechaTransito;

    @Schema(description = "Fecha y hora en que se entregó el paquete a domicilio o en sede", example = "2026-10-26T11:15:00")
    private LocalDateTime fechaEntrega;

    @Schema(description = "Fecha y hora en que el usuario canceló el pedido", example = "2026-10-24T15:00:00")
    private LocalDateTime fechaCancelacion;

    @Schema(description = "Fecha límite para recoger el pedido en sede de devolución si no fue recibido a domicilio (RF-76)", example = "2026-10-28T18:00:00")
    private LocalDateTime fechaLimiteRecogida;

    @Schema(description = "Fecha límite que tiene el usuario para devolver el libro prestado (RF-76)", example = "2026-11-09T23:59:59")
    private LocalDateTime fechaLimiteDevolucion;

    @Schema(description = "ID del usuario que solicitó el pedido", example = "1")
    private Long usuarioId;

    @Schema(description = "ID del ejemplar asignado al pedido", example = "1")
    private Long ejemplarId;

    @Schema(description = "ID del libro solicitado", example = "1")
    private Long libroId;

    @Schema(description = "Título del libro solicitado", example = "Cien años de soledad")
    private String libroTitulo;

    @Schema(description = "ID de la sede de origen de donde sale el ejemplar", example = "1")
    private Long sedeOrigenId;

    @Schema(description = "Nombre de la sede de origen", example = "Biblioteca Central Bogotá")
    private String sedeOrigenNombre;

    @Schema(description = "ID de la sede seleccionada por el usuario para realizar la devolución (RF-28)", example = "2")
    private Long sedeDevolucionId;

    @Schema(description = "Nombre de la sede elegida para la devolución", example = "Sede Norte Medellín")
    private String sedeDevolucionNombre;

    @Schema(description = "ID del repartidor asignado a la entrega o traslado", example = "5")
    private Long repartidorId;

    @Schema(description = "Nombre de usuario del repartidor asignado", example = "repartidor_juan")
    private String repartidorNombre;

    @Schema(description = "ID del préstamo vinculado al pedido", example = "1")
    private Long prestamoId;

    @Schema(description = "ID del último bibliotecario que gestionó el pedido", example = "2")
    private Long bibliotecarioId;
}
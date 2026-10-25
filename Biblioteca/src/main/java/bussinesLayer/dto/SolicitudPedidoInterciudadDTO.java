package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Solicitud de un libro de otra ciudad con entrega a domicilio y sede de devolución elegida")
public class SolicitudPedidoInterciudadDTO {

    @NotNull(message = "El campo usuarioId es obligatorio")
    @Schema(description = "ID del usuario con suscripción vigente", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long usuarioId;

    @NotNull(message = "El campo libroId es obligatorio")
    @Schema(description = "ID del libro solicitado", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long libroId;

    @NotBlank(message = "El campo ciudadEntrega es obligatorio")
    @Size(max = 80, message = "La ciudad de entrega no puede superar 80 caracteres")
    @Schema(description = "Ciudad donde se entregará el libro", example = "Medellín", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ciudadEntrega;

    @NotBlank(message = "El campo direccionEntrega es obligatorio")
    @Size(max = 200, message = "La dirección de entrega no puede superar 200 caracteres")
    @Schema(description = "Dirección exacta de entrega a domicilio", example = "Calle 10 # 20-30", requiredMode = Schema.RequiredMode.REQUIRED)
    private String direccionEntrega;

    @NotNull(message = "El campo sedeDevolucionId es obligatorio")
    @Schema(description = "ID de la sede donde el usuario devolverá el libro", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long sedeDevolucionId;

    @NotNull(message = "El campo fechaRecepcion es obligatorio")
    @FutureOrPresent(message = "La fecha de recepción debe ser hoy o una fecha futura")
    @Schema(description = "Fecha en que el usuario puede recibir el pedido (máximo 7 días)", example = "2026-10-28", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate fechaRecepcion;

    @NotBlank(message = "El campo horaRecepcion es obligatorio")
    @Schema(description = "Hora o rango de hora en que puede recibir el paquete", example = "14:00 - 16:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private String horaRecepcion;
}
package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "reserva de un libro sin stock")
public class ReservaLibroDTO {
    @Schema(description = "id unico de reserva", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull(message = "El campo fechaReserva es obligatorio")
    @Schema(description = "Fecha de reserva", example = "2026-10-17T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime fechaReserva;

    @Schema(description = "Fecha en que se aviso al usuario", example = "2026-10-18T10:00:00")
    private LocalDateTime fechaNotificacion;

    @Schema(description = "Fecha limite para retirar el libro", example = "2026-10-20T10:00:00")
    private LocalDateTime fechaLimiteRetiro;

    @Schema(description = "Fecha de cancelacion", example = "2026-10-19T10:00:00")
    private LocalDateTime fechaCancelacion;

    @Size(max = 20, message = "El campo estado no puede superar los 20 caracteres")
    @Schema(description = "EN_ESPERA, NOTIFICADA, RETIRADA, CANCELADA, EXPIRADA", example = "EN_ESPERA", maxLength = 20)
    private String estado;

    @NotNull(message = "El campo usuarioId es obligatorio")
    @Schema(description = "id del usuario", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long usuarioId;

    @NotNull(message = "El campo libroId es obligatorio")
    @Schema(description = "id del libro", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long libroId;

    @NotNull(message = "El campo sedeId es obligatorio")
    @Schema(description = "id de la sede", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long sedeId;

    @Schema(description = "id del ejemplar apartado", example = "1")
    private Long ejemplarId;

}

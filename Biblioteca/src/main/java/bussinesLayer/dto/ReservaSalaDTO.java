package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "reserva de una sala de estudio")
public class ReservaSalaDTO {
    @Schema(description = "id unico de reserva", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull(message = "El campo fechaInicio es obligatorio")
    @Schema(description = "Fecha de inicio", example = "2026-10-20T14:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime fechaInicio;

    @NotNull(message = "El campo fechaFin es obligatorio")
    @Schema(description = "Fecha de fin", example = "2026-10-20T16:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime fechaFin;

    @Size(max = 20, message = "El campo estado no puede superar los 20 caracteres")
    @Schema(description = "PENDIENTE_PAGO, CONFIRMADA, CANCELADA, FINALIZADA", example = "PENDIENTE_PAGO", maxLength = 20)
    private String estado;

    @NotNull(message = "El campo valorBase es obligatorio")
    @DecimalMin(value = "0.00", message = "El campo valorBase no puede ser negativo")
    @Schema(description = "Valor base", example = "20000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal valorBase;

    @DecimalMin(value = "0.00", message = "El campo descuento no puede ser negativo")
    @Schema(description = "Descuento por suscripcion", example = "0.00")
    private BigDecimal descuento;

    @NotNull(message = "El campo valorTotal es obligatorio")
    @DecimalMin(value = "0.00", message = "El campo valorTotal no puede ser negativo")
    @Schema(description = "Valor total", example = "20000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal valorTotal;

    @NotNull(message = "El campo fechaCreacion es obligatorio")
    @Schema(description = "Fecha de creacion", example = "2026-10-17T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime fechaCreacion;

    @Schema(description = "Fecha de cancelacion", example = "2026-10-19T10:00:00")
    private LocalDateTime fechaCancelacion;

    @NotNull(message = "El campo usuarioId es obligatorio")
    @Schema(description = "id del usuario", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long usuarioId;

    @NotNull(message = "El campo salaId es obligatorio")
    @Schema(description = "id de la sala de estudio", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long salaId;

}

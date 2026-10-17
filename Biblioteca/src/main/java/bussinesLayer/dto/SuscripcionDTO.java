package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "suscripcion de un usuario a un plan")
public class SuscripcionDTO {
    @Schema(description = "id unico de suscripcion", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull(message = "El campo fechaInicio es obligatorio")
    @Schema(description = "Fecha de inicio", example = "2026-10-17", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate fechaInicio;

    @NotNull(message = "El campo fechaFin es obligatorio")
    @Schema(description = "Fecha de fin", example = "2026-11-17", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate fechaFin;

    @Size(max = 20, message = "El campo estado no puede superar los 20 caracteres")
    @Schema(description = "ACTIVA, PAUSADA, CANCELADA, VENCIDA", example = "ACTIVA", maxLength = 20)
    private String estado;

    @NotNull(message = "El campo valorMensual es obligatorio")
    @DecimalMin(value = "0.00", message = "El campo valorMensual no puede ser negativo")
    @Schema(description = "Valor mensual", example = "30000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal valorMensual;

    @Schema(description = "Renovacion automatica", example = "true")
    private Boolean renovacionAutomatica;

    @Schema(description = "Fecha en que se solicito cancelar", example = "2026-11-01T10:00:00")
    private LocalDateTime fechaCancelacion;

    @Min(value = 0, message = "El campo mesesConsecutivos no puede ser negativo")
    @Schema(description = "Meses consecutivos pagados", example = "0")
    private Integer mesesConsecutivos;

    @NotNull(message = "El campo usuarioId es obligatorio")
    @Schema(description = "id del usuario", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long usuarioId;

    @NotNull(message = "El campo planId es obligatorio")
    @Schema(description = "id del plan de suscripcion", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long planId;

}

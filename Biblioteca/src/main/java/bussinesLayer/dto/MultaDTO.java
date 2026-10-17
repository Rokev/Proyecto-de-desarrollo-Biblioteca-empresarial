package BussisnesCatLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "multa generada a un usuario")
public class MultaDTO {
    @Schema(description = "id unico de multa", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull(message = "El campo monto es obligatorio")
    @DecimalMin(value = "0.00", message = "El campo monto no puede ser negativo")
    @Schema(description = "Monto de la multa", example = "5000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal monto;

    @NotBlank(message = "El campo motivo es obligatorio")
    @Size(max = 200, message = "El campo motivo no puede superar los 200 caracteres")
    @Schema(description = "Motivo de la multa", example = "Devolucion atrasada", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
    private String motivo;

    @NotNull(message = "El campo fechaGeneracion es obligatorio")
    @Schema(description = "Fecha de generacion", example = "2026-10-17T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime fechaGeneracion;

    @Size(max = 20, message = "El campo estado no puede superar los 20 caracteres")
    @Schema(description = "PENDIENTE, PAGADA, ANULADA", example = "PENDIENTE", maxLength = 20)
    private String estado;

    @NotNull(message = "El campo usuarioId es obligatorio")
    @Schema(description = "id del usuario", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long usuarioId;

    @Schema(description = "id del prestamo que origino la multa", example = "1")
    private Long prestamoId;

}

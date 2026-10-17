package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "sancion aplicada a un usuario")
public class SancionDTO {
    @Schema(description = "id unico de sancion", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "El campo tipo es obligatorio")
    @Size(max = 40, message = "El campo tipo no puede superar los 40 caracteres")
    @Schema(description = "BLOQUEO_PRESTAMOS, SUSPENSION, ADVERTENCIA, etc.", example = "BLOQUEO_PRESTAMOS", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 40)
    private String tipo;

    @NotBlank(message = "El campo motivo es obligatorio")
    @Size(max = 200, message = "El campo motivo no puede superar los 200 caracteres")
    @Schema(description = "Motivo de la sancion", example = "Retraso en la devolucion", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
    private String motivo;

    @NotNull(message = "El campo fechaAplicacion es obligatorio")
    @Schema(description = "Fecha de aplicacion", example = "2026-10-17T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime fechaAplicacion;

    @Schema(description = "Fin de la restriccion", example = "2026-10-24T10:00:00")
    private LocalDateTime fechaFin;

    @Size(max = 20, message = "El campo estado no puede superar los 20 caracteres")
    @Schema(description = "ACTIVA, FINALIZADA, ANULADA", example = "ACTIVA", maxLength = 20)
    private String estado;

    @NotNull(message = "El campo usuarioId es obligatorio")
    @Schema(description = "id del usuario", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long usuarioId;

    @Schema(description = "id del prestamo que origino la sancion", example = "1")
    private Long prestamoId;

}

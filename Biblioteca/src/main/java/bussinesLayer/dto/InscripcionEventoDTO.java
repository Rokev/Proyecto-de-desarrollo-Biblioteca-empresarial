package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "inscripcion de un usuario a un evento")
public class InscripcionEventoDTO {
    @Schema(description = "id unico de inscripcion", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull(message = "El campo fechaInscripcion es obligatorio")
    @Schema(description = "Fecha de inscripcion", example = "2026-10-17T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime fechaInscripcion;

    @Min(value = 1, message = "El campo cantidadCupos debe ser al menos 1")
    @Schema(description = "Cantidad de cupos reservados", example = "1")
    private Integer cantidadCupos;

    @Schema(description = "Fecha de cancelacion", example = "2026-10-20T10:00:00")
    private LocalDateTime fechaCancelacion;

    @Size(max = 20, message = "El campo estado no puede superar los 20 caracteres")
    @Schema(description = "ACTIVA, CANCELADA", example = "ACTIVA", maxLength = 20)
    private String estado;

    @NotNull(message = "El campo eventoId es obligatorio")
    @Schema(description = "id del evento", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long eventoId;

    @NotNull(message = "El campo usuarioId es obligatorio")
    @Schema(description = "id del usuario", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long usuarioId;

}

package BussisnesCatLayer.dto;

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
@Schema(description = "justificacion de un retraso en la devolucion")
public class JustificacionRetrasoDTO {
    @Schema(description = "id unico de justificacion", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "El campo texto es obligatorio")
    @Schema(description = "Texto de la justificacion", example = "Tuve una emergencia medica", requiredMode = Schema.RequiredMode.REQUIRED)
    private String texto;

    @NotNull(message = "El campo fechaEnvio es obligatorio")
    @Schema(description = "Fecha de envio", example = "2026-10-17T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime fechaEnvio;

    @Size(max = 20, message = "El campo estado no puede superar los 20 caracteres")
    @Schema(description = "PENDIENTE, ACEPTADA, RECHAZADA", example = "PENDIENTE", maxLength = 20)
    private String estado;

    @Schema(description = "Respuesta del bibliotecario", example = "Justificacion aceptada")
    private String respuesta;

    @Schema(description = "Fecha de respuesta", example = "2026-10-18T10:00:00")
    private LocalDateTime fechaRespuesta;

    @NotNull(message = "El campo prestamoId es obligatorio")
    @Schema(description = "id del prestamo", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long prestamoId;

    @NotNull(message = "El campo usuarioId es obligatorio")
    @Schema(description = "id del usuario", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long usuarioId;

    @Schema(description = "id del bibliotecario al que se envia", example = "1")
    private Long bibliotecarioId;

}

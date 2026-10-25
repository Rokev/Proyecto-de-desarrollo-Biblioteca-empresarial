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
@Schema(description = "notificacion enviada a un usuario")
public class NotificacionDTO {
    @Schema(description = "id unico de notificacion", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "El campo tipo es obligatorio")
    @Size(max = 40, message = "El campo tipo no puede superar los 40 caracteres")
    @Schema(description = "LIBRO_DISPONIBLE, RECUPERAR_CONTRASENA, etc.", example = "LIBRO_DISPONIBLE", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 40)
    private String tipo;

    @NotBlank(message = "El campo titulo es obligatorio")
    @Size(max = 150, message = "El campo titulo no puede superar los 150 caracteres")
    @Schema(description = "Titulo", example = "Tu libro esta disponible", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 150)
    private String titulo;

    @NotBlank(message = "El campo mensaje es obligatorio")
    @Size(max = 500, message = "El campo mensaje no puede superar los 500 caracteres")
    @Schema(description = "Mensaje", example = "Ya puedes retirar tu libro reservado", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 500)
    private String mensaje;

    @Size(max = 20, message = "El campo canal no puede superar los 20 caracteres")
    @Schema(description = "CORREO o APP", example = "APP", maxLength = 20)
    private String canal;

    @NotNull(message = "El campo fechaCreacion es obligatorio")
    @Schema(description = "Fecha de creacion", example = "2026-10-17T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime fechaCreacion;

    @Schema(description = "Indica si fue leida", example = "false")
    private Boolean leida;

    @NotNull(message = "El campo usuarioId es obligatorio")
    @Schema(description = "id del usuario", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long usuarioId;

}

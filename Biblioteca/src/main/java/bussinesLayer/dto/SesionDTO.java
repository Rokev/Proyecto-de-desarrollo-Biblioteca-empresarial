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
@Schema(description = "sesion activa de un usuario")
public class SesionDTO {
    @Schema(description = "id unico de sesion", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "El campo token es obligatorio")
    @Size(max = 255, message = "El campo token no puede superar los 255 caracteres")
    @Schema(description = "Token de la sesion", example = "eyJhbGciOi...", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 255)
    private String token;

    @NotNull(message = "El campo fechaInicio es obligatorio")
    @Schema(description = "Fecha de inicio", example = "2026-10-17T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime fechaInicio;

    @NotNull(message = "El campo fechaExpiracion es obligatorio")
    @Schema(description = "Fecha de expiracion", example = "2026-10-17T22:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime fechaExpiracion;

    @Schema(description = "Fecha de cierre", example = "2026-10-17T18:00:00")
    private LocalDateTime fechaCierre;

    @Schema(description = "Indica si la sesion esta activa", example = "true")
    private Boolean activa;

    @NotNull(message = "El campo usuarioId es obligatorio")
    @Schema(description = "id del usuario", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long usuarioId;

}

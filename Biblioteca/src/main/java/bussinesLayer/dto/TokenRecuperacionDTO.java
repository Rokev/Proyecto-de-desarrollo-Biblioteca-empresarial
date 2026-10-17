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
@Schema(description = "token de restablecimiento de contrasena")
public class TokenRecuperacionDTO {
    @Schema(description = "id unico de token", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "El campo token es obligatorio")
    @Size(max = 100, message = "El campo token no puede superar los 100 caracteres")
    @Schema(description = "Token de recuperacion", example = "a1b2c3d4", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
    private String token;

    @NotNull(message = "El campo fechaCreacion es obligatorio")
    @Schema(description = "Fecha de creacion", example = "2026-10-17T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime fechaCreacion;

    @NotNull(message = "El campo fechaExpiracion es obligatorio")
    @Schema(description = "Fecha de expiracion (creacion + 24 horas)", example = "2026-10-18T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime fechaExpiracion;

    @Schema(description = "Indica si ya fue usado", example = "false")
    private Boolean usado;

    @NotNull(message = "El campo usuarioId es obligatorio")
    @Schema(description = "id del usuario", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long usuarioId;

}

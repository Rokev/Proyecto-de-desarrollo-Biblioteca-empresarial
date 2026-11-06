package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "respuesta del inicio de sesion con el token JWT")
public class AuthResponseDTO {

    @Schema(description = "Token JWT para el encabezado Authorization: Bearer <token>")
    private String token;

    @Schema(description = "Tipo de token", example = "Bearer")
    private String tipo;

    @Schema(description = "Fecha de expiracion del token")
    private LocalDateTime expiracion;

    @Schema(description = "id del usuario", example = "1")
    private Long usuarioId;

    @Schema(description = "Nombre del usuario", example = "Maria")
    private String nombre;

    @Schema(description = "Rol del usuario", example = "CLIENTE")
    private String rol;
}
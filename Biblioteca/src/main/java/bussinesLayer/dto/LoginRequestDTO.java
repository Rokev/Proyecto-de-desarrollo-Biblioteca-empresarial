package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "credenciales de inicio de sesion")
public class LoginRequestDTO {

    @NotBlank(message = "El campo correo es obligatorio")
    @Email(message = "El correo no tiene un formato valido")
    @Schema(description = "Correo electronico", example = "admin@libroya.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String correo;

    @NotBlank(message = "La contrasena es obligatoria")
    @Schema(description = "Contrasena", example = "Admin12345", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contrasena;
}
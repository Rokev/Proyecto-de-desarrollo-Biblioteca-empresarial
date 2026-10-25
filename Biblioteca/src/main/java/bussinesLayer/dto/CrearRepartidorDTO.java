package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO para la creación de una cuenta con rol Repartidor por parte del Administrador (RF-58)")
public class CrearRepartidorDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
    @Schema(description = "Nombre del repartidor", example = "Carlos", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 80, message = "El apellido no puede superar los 80 caracteres")
    @Schema(description = "Apellido del repartidor", example = "Pérez", requiredMode = Schema.RequiredMode.REQUIRED)
    private String apellido;

    @NotBlank(message = "El documento es obligatorio")
    @Size(max = 20, message = "El documento no puede superar los 20 caracteres")
    @Schema(description = "Documento de identidad único", example = "1098765432", requiredMode = Schema.RequiredMode.REQUIRED)
    private String documento;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 40, message = "El nombre de usuario no puede superar los 40 caracteres")
    @Schema(description = "Nombre de usuario único para inicio de sesión", example = "repartidor_carlos", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nombreUsuario;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener un formato válido")
    @Size(max = 120, message = "El correo no puede superar los 120 caracteres")
    @Schema(description = "Correo electrónico institucional o de contacto", example = "carlos.repartidor@biblioteca.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener mínimo 8 caracteres")
    @Schema(description = "Contraseña inicial de la cuenta (mínimo 8 caracteres)", example = "ClaveSegura123*", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contrasena;

    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    @Schema(description = "Teléfono móvil del repartidor", example = "3001234567")
    private String telefono;
}
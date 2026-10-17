package bussinesLayer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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
@Schema(description = "informacion del usuario")
public class UsuarioDTO {
    @Schema(description = "id unico de usuario", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "El campo nombre es obligatorio")
    @Size(max = 80, message = "El campo nombre no puede superar los 80 caracteres")
    @Schema(description = "Nombre", example = "Maria", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 80)
    private String nombre;

    @NotBlank(message = "El campo apellido es obligatorio")
    @Size(max = 80, message = "El campo apellido no puede superar los 80 caracteres")
    @Schema(description = "Apellido", example = "Perez", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 80)
    private String apellido;

    @NotBlank(message = "El campo documento es obligatorio")
    @Size(max = 20, message = "El campo documento no puede superar los 20 caracteres")
    @Schema(description = "Documento de identidad", example = "1020304050", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 20)
    private String documento;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 40, message = "El campo nombreUsuario no puede superar los 40 caracteres")
    @Schema(description = "Nombre de usuario unico", example = "maria12", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 40)
    private String nombreUsuario;

    @NotBlank(message = "El campo correo es obligatorio")
    @Size(max = 120, message = "El campo correo no puede superar los 120 caracteres")
    @Email(message = "El correo no tiene un formato valido")
    @Schema(description = "Correo electronico", example = "maria12@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 120)
    private String correo;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 8, max = 72, message = "La contrasena debe tener minimo 8 caracteres")
    @Schema(description = "Contrasena (minimo 8 caracteres). Solo se recibe, nunca se devuelve", example = "Clave1234", accessMode = Schema.AccessMode.WRITE_ONLY, minLength = 8)
    private String contrasena;

    @Size(max = 20, message = "El campo telefono no puede superar los 20 caracteres")
    @Schema(description = "Telefono", example = "3001234567", maxLength = 20)
    private String telefono;

    @Size(max = 200, message = "El campo direccion no puede superar los 200 caracteres")
    @Schema(description = "Direccion", example = "calle 35 casa n34-57", maxLength = 200)
    private String direccion;

    @Schema(description = "Fecha de registro", example = "2026-10-17T10:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime fechaRegistro;

    @Schema(description = "Indica si el usuario esta activo", example = "true")
    private Boolean activo;

    @NotNull(message = "El campo rolId es obligatorio")
    @Schema(description = "id del rol", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long rolId;

    @Schema(description = "id de la sede donde trabaja (solo personal)", example = "1")
    private Long sedeId;

}

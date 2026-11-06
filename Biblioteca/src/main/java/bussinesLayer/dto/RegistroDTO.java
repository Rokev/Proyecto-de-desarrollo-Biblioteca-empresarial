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
@Schema(description = "datos para el auto-registro de un cliente (el rol siempre es CLIENTE)")
public class RegistroDTO {

    @NotBlank(message = "El campo nombre es obligatorio")
    @Size(max = 80, message = "El campo nombre no puede superar los 80 caracteres")
    private String nombre;

    @NotBlank(message = "El campo apellido es obligatorio")
    @Size(max = 80, message = "El campo apellido no puede superar los 80 caracteres")
    private String apellido;

    @NotBlank(message = "El campo documento es obligatorio")
    @Size(max = 20, message = "El campo documento no puede superar los 20 caracteres")
    private String documento;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 40, message = "El campo nombreUsuario no puede superar los 40 caracteres")
    private String nombreUsuario;

    @NotBlank(message = "El campo correo es obligatorio")
    @Size(max = 120, message = "El campo correo no puede superar los 120 caracteres")
    @Email(message = "El correo no tiene un formato valido")
    private String correo;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
    private String contrasena;

    @Size(max = 20, message = "El campo telefono no puede superar los 20 caracteres")
    private String telefono;

    @Size(max = 200, message = "El campo direccion no puede superar los 200 caracteres")
    private String direccion;
}
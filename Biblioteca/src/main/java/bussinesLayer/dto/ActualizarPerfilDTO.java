package bussinesLayer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActualizarPerfilDTO {

    @NotBlank(message = "El campo nombre es obligatorio")
    @Size(max = 80, message = "El campo nombre no puede superar los 80 caracteres")
    private String nombre;

    @NotBlank(message = "El campo apellido es obligatorio")
    @Size(max = 80, message = "El campo apellido no puede superar los 80 caracteres")
    private String apellido;

    @Size(max = 20, message = "El campo telefono no puede superar los 20 caracteres")
    private String telefono;

    @Size(max = 200, message = "El campo direccion no puede superar los 200 caracteres")
    private String direccion;
    
    @Size(max = 50, message = "El campo nombreUsuario no puede superar los 50 caracteres")
    private String nombreUsuario;
}
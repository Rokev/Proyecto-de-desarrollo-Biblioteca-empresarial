package bussinesLayer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecuperarContrasenaDTO {

    @NotBlank(message = "El campo correo es obligatorio")
    @Email(message = "El correo no tiene un formato valido")
    private String correo;
}
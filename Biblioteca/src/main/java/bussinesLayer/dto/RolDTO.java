package BussisnesCatLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "rol de un usuario")
public class RolDTO {
    @Schema(description = "id unico de rol", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "El campo nombre es obligatorio")
    @Size(max = 30, message = "El campo nombre no puede superar los 30 caracteres")
    @Schema(description = "Nombre del rol", example = "CLIENTE", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 30)
    private String nombre;

    @Size(max = 200, message = "El campo descripcion no puede superar los 200 caracteres")
    @Schema(description = "Descripcion", example = "Usuario cliente de la biblioteca", maxLength = 200)
    private String descripcion;

}

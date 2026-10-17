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
@Schema(description = "autor del catalogo")
public class AutorDTO {
    @Schema(description = "id unico de autor", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "El campo nombre es obligatorio")
    @Size(max = 120, message = "El campo nombre no puede superar los 120 caracteres")
    @Schema(description = "Nombre del autor", example = "Gabriel Garcia Marquez", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 120)
    private String nombre;

    @Size(max = 80, message = "El campo nacionalidad no puede superar los 80 caracteres")
    @Schema(description = "Nacionalidad", example = "Colombiano", maxLength = 80)
    private String nacionalidad;

    @Schema(description = "Biografia del autor", example = "Premio Nobel de Literatura")
    private String biografia;

}

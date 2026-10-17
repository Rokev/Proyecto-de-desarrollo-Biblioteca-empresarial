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
@Schema(description = "genero literario")
public class GeneroDTO {
    @Schema(description = "id unico de genero", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "El campo nombre es obligatorio")
    @Size(max = 60, message = "El campo nombre no puede superar los 60 caracteres")
    @Schema(description = "Nombre del genero", example = "Novela", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 60)
    private String nombre;

    @Size(max = 200, message = "El campo descripcion no puede superar los 200 caracteres")
    @Schema(description = "Descripcion", example = "Narrativa extensa", maxLength = 200)
    private String descripcion;

}

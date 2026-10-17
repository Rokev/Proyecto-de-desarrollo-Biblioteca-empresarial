package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "libro del catalogo")
public class LibroDTO {
    @Schema(description = "id unico de libro", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "El campo isbn es obligatorio")
    @Size(max = 20, message = "El campo isbn no puede superar los 20 caracteres")
    @Schema(description = "ISBN del libro", example = "9780307474728", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 20)
    private String isbn;

    @NotBlank(message = "El campo titulo es obligatorio")
    @Size(max = 200, message = "El campo titulo no puede superar los 200 caracteres")
    @Schema(description = "Titulo", example = "Cien anos de soledad", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
    private String titulo;

    @NotBlank(message = "El campo idioma es obligatorio")
    @Size(max = 40, message = "El campo idioma no puede superar los 40 caracteres")
    @Schema(description = "Idioma", example = "Espanol", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 40)
    private String idioma;

    @NotNull(message = "El campo anioPublicacion es obligatorio")
    @Schema(description = "Anio de publicacion", example = "1967", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer anioPublicacion;

    @Size(max = 120, message = "El campo editorial no puede superar los 120 caracteres")
    @Schema(description = "Editorial", example = "Sudamericana", maxLength = 120)
    private String editorial;

    @Schema(description = "Sinopsis", example = "La historia de la familia Buendia")
    private String sinopsis;

    @Min(value = 1, message = "El campo numeroPaginas debe ser al menos 1")
    @Schema(description = "Numero de paginas", example = "471")
    private Integer numeroPaginas;

    @Size(max = 300, message = "El campo portadaUrl no puede superar los 300 caracteres")
    @Schema(description = "URL de la portada", example = "https://ejemplo.com/portada.jpg", maxLength = 300)
    private String portadaUrl;

    @NotNull(message = "El campo generoId es obligatorio")
    @Schema(description = "id del genero", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long generoId;

    @Schema(description = "ids de los autores", example = "[1, 2]")
    private List<Long> autoresIds;

}

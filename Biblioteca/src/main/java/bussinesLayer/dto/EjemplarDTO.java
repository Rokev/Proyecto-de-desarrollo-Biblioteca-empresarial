package BussisnesCatLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "copia fisica de un libro")
public class EjemplarDTO {
    @Schema(description = "id unico de ejemplar", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "El campo codigoBarras es obligatorio")
    @Size(max = 40, message = "El campo codigoBarras no puede superar los 40 caracteres")
    @Schema(description = "Codigo de barras / inventario", example = "EJ-000123", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 40)
    private String codigoBarras;

    @Size(max = 20, message = "El campo estado no puede superar los 20 caracteres")
    @Schema(description = "DISPONIBLE, RESERVADO, PRESTADO, EN_TRANSITO, DANADO", example = "DISPONIBLE", maxLength = 20)
    private String estado;

    @NotNull(message = "El campo libroId es obligatorio")
    @Schema(description = "id del libro", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long libroId;

    @NotNull(message = "El campo sedeId es obligatorio")
    @Schema(description = "id de la sede donde se encuentra la copia", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long sedeId;

}

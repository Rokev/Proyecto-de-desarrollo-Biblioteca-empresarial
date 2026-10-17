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
@Schema(description = "pregunta frecuente para el chatbot")
public class PreguntaFrecuenteDTO {
    @Schema(description = "id unico de pregunta", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "El campo pregunta es obligatorio")
    @Size(max = 250, message = "El campo pregunta no puede superar los 250 caracteres")
    @Schema(description = "Pregunta", example = "Cual es el horario de atencion?", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 250)
    private String pregunta;

    @NotBlank(message = "El campo respuesta es obligatorio")
    @Schema(description = "Respuesta", example = "De lunes a viernes de 8am a 6pm", requiredMode = Schema.RequiredMode.REQUIRED)
    private String respuesta;

    @Size(max = 60, message = "El campo categoria no puede superar los 60 caracteres")
    @Schema(description = "Categoria", example = "Horarios", maxLength = 60)
    private String categoria;

    @Schema(description = "Indica si esta activa", example = "true")
    private Boolean activa;

}

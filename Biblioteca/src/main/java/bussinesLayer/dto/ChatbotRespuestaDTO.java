package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Respuesta del chatbot")
public class ChatbotRespuestaDTO {

    @Schema(description = "Texto que se le muestra al usuario")
    private String respuesta;

    @Schema(description = "Pregunta frecuente que coincidio (null si no se encontro respuesta)")
    private String preguntaCoincidente;

    @Schema(description = "Categoria de la pregunta que coincidio", example = "Horarios")
    private String categoria;

    @Schema(description = "Que tan parecida fue la coincidencia, de 0 a 1", example = "0.75")
    private double coincidencia;

    @Schema(description = "Si no hubo coincidencia, preguntas que el usuario puede intentar")
    private List<String> sugerencias;
}

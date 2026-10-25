package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Mensaje que el usuario le escribe al chatbot")
public class ChatbotMensajeDTO {

    @NotBlank(message = "El campo mensaje es obligatorio")
    @Size(max = 500, message = "El mensaje no puede superar los 500 caracteres")
    @Schema(description = "Texto escrito por el usuario", example = "A que hora abre la biblioteca?",
            requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 500)
    private String mensaje;
}

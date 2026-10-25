package presentationLayer.controller;

import bussinesLayer.dto.ChatbotMensajeDTO;
import bussinesLayer.dto.ChatbotRespuestaDTO;
import bussinesLayer.service.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import presentationLayer.advice.ApiError;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chatbot", description = "Asistente que responde con la base de preguntas frecuentes")
class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/mensaje")
    @Operation(summary = "Enviar un mensaje al chatbot",
            description = "Busca la pregunta frecuente activa mas parecida al mensaje y devuelve su respuesta. "
                    + "Si ninguna se parece lo suficiente, devuelve sugerencias.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Respuesta del chatbot",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatbotRespuestaDTO.class))),
            @ApiResponse(responseCode = "400", description = "Mensaje vacio o demasiado largo",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ChatbotRespuestaDTO> enviarMensaje(@Valid @RequestBody ChatbotMensajeDTO mensaje) {
        log.debug("POST /api/v1/chatbot/mensaje");
        return ResponseEntity.ok(chatbotService.responder(mensaje.getMensaje()));
    }
}

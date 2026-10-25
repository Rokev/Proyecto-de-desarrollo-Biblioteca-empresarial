package bussinesLayer.service;

import bussinesLayer.dto.ChatbotRespuestaDTO;

/** Chatbot sencillo: responde con la pregunta frecuente mas parecida al mensaje */
public interface ChatbotService {

    ChatbotRespuestaDTO responder(String mensaje);
}

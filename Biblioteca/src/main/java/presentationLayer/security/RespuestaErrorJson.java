package presentationLayer.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.LocalDateTime;

/** Escribe un error con el formato ApiError directamente en la respuesta (filtros de seguridad). */
public final class RespuestaErrorJson {

    private RespuestaErrorJson() {
    }

    public static void escribir(HttpServletResponse response, HttpStatus status, String mensaje, String ruta)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String json = "{\"timestamp\":\"" + LocalDateTime.now() + "\","
                + "\"status\":" + status.value() + ","
                + "\"error\":\"" + escapar(status.getReasonPhrase()) + "\","
                + "\"message\":\"" + escapar(mensaje) + "\","
                + "\"path\":\"" + escapar(ruta) + "\"}";
        response.getWriter().write(json);
    }

    private static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }
}
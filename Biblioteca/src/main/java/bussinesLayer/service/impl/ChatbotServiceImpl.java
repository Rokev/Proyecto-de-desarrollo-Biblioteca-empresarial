package bussinesLayer.service.impl;

import bussinesLayer.dto.ChatbotRespuestaDTO;
import bussinesLayer.dto.PreguntaFrecuenteDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.service.ChatbotService;
import bussinesLayer.service.ConfiguracionSistemaService;
import bussinesLayer.util.VariablesConfiguracion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.PreguntaFrecuenteDAO;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sin servicios externos: normaliza el mensaje (minusculas, sin tildes, sin palabras vacias) y lo compara
 * con cada pregunta frecuente activa. Gana la que comparte mas palabras con el mensaje; la base de
 * conocimiento se administra con /api/v1/preguntas-frecuentes.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ChatbotServiceImpl implements ChatbotService {

    /** Por debajo de este valor se considera que el chatbot no entendio. */
    static final double UMBRAL_COINCIDENCIA = 0.34;
    private static final int MAX_SUGERENCIAS = 3;
    private static final int RAIZ_LETRAS = 5;

    private static final Set<String> PALABRAS_VACIAS = Set.of(
            "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "al", "a", "en", "y", "o", "u",
            "que", "es", "son", "se", "por", "para", "con", "sin", "mi", "me", "tu", "su", "lo", "le", "les",
            "como", "cual", "cuales", "cuando", "donde", "quiero", "puedo", "hay", "tiene", "tienen", "ser",
            "hola", "buenas", "buenos", "tardes", "noches", "favor", "porfavor", "gracias");

    /** Palabras que la gente usa distinto a como esta escrita la pregunta frecuente. */
    private static final Map<String, String> SINONIMOS = Map.of(
            "hora", "horario", "abren", "horario", "abre", "horario", "abierta", "horario",
            "tiempo", "dia", "plazo", "dia", "durar", "dura");

    private final PreguntaFrecuenteDAO preguntaFrecuenteDAO;
    private final ConfiguracionSistemaService configuracionService;

    @Override
    public ChatbotRespuestaDTO responder(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            throw new BusinessValidationException("El mensaje no puede estar vacio");
        }
        Set<String> palabrasMensaje = palabrasClave(mensaje);
        List<PreguntaFrecuenteDTO> base = preguntaFrecuenteDAO.findByActivaTrue();

        PreguntaFrecuenteDTO mejor = null;
        double mejorPuntaje = 0;
        for (PreguntaFrecuenteDTO faq : base) {
            double puntaje = puntuar(palabrasMensaje, faq);
            if (puntaje > mejorPuntaje) {
                mejorPuntaje = puntaje;
                mejor = faq;
            }
        }

        if (mejor != null && mejorPuntaje >= UMBRAL_COINCIDENCIA) {
            log.info("Chatbot: coincidencia {} ({})", mejor.getId(), mejorPuntaje);
            // Las variables {DIAS_PRESTAMO}, {MULTA_MORA_POR_DIA}... se resuelven con la configuracion vigente ( 74/75/76).
            return new ChatbotRespuestaDTO(VariablesConfiguracion.resolver(mejor.getRespuesta(), configuracionService),
                    mejor.getPregunta(), mejor.getCategoria(),
                    redondear(mejorPuntaje), List.of());
        }

        log.info("Chatbot: sin coincidencia para el mensaje recibido");
        List<String> sugerencias = base.stream()
                .sorted(Comparator.comparingDouble((PreguntaFrecuenteDTO f) -> puntuar(palabrasMensaje, f)).reversed())
                .limit(MAX_SUGERENCIAS)
                .map(PreguntaFrecuenteDTO::getPregunta)
                .toList();
        return new ChatbotRespuestaDTO(
                "No encontre una respuesta para tu consulta. Intenta con otras palabras o pregunta por alguno de estos temas.",
                null, null, redondear(mejorPuntaje), sugerencias);
    }

    /** Fraccion de las palabras de la pregunta (y su categoria) que aparecen en el mensaje. */
    private double puntuar(Set<String> palabrasMensaje, PreguntaFrecuenteDTO faq) {
        Set<String> palabrasFaq = palabrasClave(faq.getPregunta());
        if (faq.getCategoria() != null) {
            palabrasFaq.addAll(palabrasClave(faq.getCategoria()));
        }
        if (palabrasFaq.isEmpty() || palabrasMensaje.isEmpty()) {
            return 0;
        }
        long comunes = palabrasMensaje.stream().filter(palabrasFaq::contains).count();
        // Se promedia cobertura de la pregunta y del mensaje para no premiar mensajes muy largos ni muy cortos.
        double cobertura = (double) comunes / palabrasFaq.size();
        double precision = (double) comunes / palabrasMensaje.size();
        return (cobertura + precision) / 2;
    }

    private Set<String> palabrasClave(String texto) {
        String limpio = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9\\s]", " ");
        return Arrays.stream(limpio.split("\\s+"))
                .filter(p -> p.length() > 1 && !PALABRAS_VACIAS.contains(p))
                .map(p -> SINONIMOS.getOrDefault(p, p))
                .map(ChatbotServiceImpl::raiz)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /** Deja solo el inicio de la palabra: "cancelar", "cancelo" y "cancelacion" cuentan como la misma. */
    private static String raiz(String palabra) {
        return palabra.length() > RAIZ_LETRAS ? palabra.substring(0, RAIZ_LETRAS) : palabra;
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}

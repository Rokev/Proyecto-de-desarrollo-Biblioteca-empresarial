package bussinesLayer.service.impl;

import bussinesLayer.dto.ChatbotRespuestaDTO;
import bussinesLayer.dto.PreguntaFrecuenteDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.service.ConfiguracionSistemaService;
import bussinesLayer.util.ClavePlazo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.PreguntaFrecuenteDAO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatbotServiceImpl")
class ChatbotServiceImplTest {

    private static final String PREGUNTA_HORARIO = "Cual es el horario de atencion?";
    private static final String RESPUESTA_HORARIO = "De lunes a viernes de 8am a 6pm";

    @Mock private PreguntaFrecuenteDAO preguntaFrecuenteDAO;
    @Mock private ConfiguracionSistemaService configuracionService;

    @InjectMocks
    private ChatbotServiceImpl service;

    private PreguntaFrecuenteDTO faq(Long id, String pregunta, String respuesta, String categoria) {
        return new PreguntaFrecuenteDTO(id, pregunta, respuesta, categoria, true);
    }

    private PreguntaFrecuenteDTO faqHorario() {
        return faq(1L, PREGUNTA_HORARIO, RESPUESTA_HORARIO, "Horarios");
    }

    private PreguntaFrecuenteDTO faqPrestamo() {
        return faq(2L, "Cuantos dias dura el prestamo?", "El prestamo dura {DIAS_PRESTAMO} dias", "Prestamos");
    }

    // ---------------------------------------------------------------- validación del mensaje

    @Test
    @DisplayName("responder: mensaje nulo lanza BusinessValidationException")
    void responder_mensajeNulo_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class, () -> service.responder(null));

        verifyNoInteractions(preguntaFrecuenteDAO);
    }

    @Test
    @DisplayName("responder: mensaje en blanco lanza BusinessValidationException")
    void responder_mensajeEnBlanco_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class, () -> service.responder("   "));

        verifyNoInteractions(preguntaFrecuenteDAO);
    }

    // ---------------------------------------------------------------- coincidencias

    @Test
    @DisplayName("responder: mensaje igual a la pregunta devuelve su respuesta con coincidencia 1.0")
    void responder_coincidenciaExacta_devuelveRespuesta() {
        when(preguntaFrecuenteDAO.findByActivaTrue()).thenReturn(List.of(faqHorario()));

        ChatbotRespuestaDTO resultado = service.responder("horario de atencion");

        assertEquals(RESPUESTA_HORARIO, resultado.getRespuesta());
        assertEquals(PREGUNTA_HORARIO, resultado.getPreguntaCoincidente());
        assertEquals("Horarios", resultado.getCategoria());
        assertEquals(1.0, resultado.getCoincidencia());
        assertTrue(resultado.getSugerencias().isEmpty());
    }

    @Test
    @DisplayName("responder: ignora tildes, mayúsculas y signos de puntuación")
    void responder_ignoraTildesMayusculasYPuntuacion() {
        when(preguntaFrecuenteDAO.findByActivaTrue()).thenReturn(List.of(faqHorario()));

        ChatbotRespuestaDTO resultado = service.responder("¡HORARIO de ATENCIÓN!!!");

        assertEquals(RESPUESTA_HORARIO, resultado.getRespuesta());
        assertEquals(1.0, resultado.getCoincidencia());
    }

    @Test
    @DisplayName("responder: reconoce sinónimos ('hora', 'abren') como 'horario'")
    void responder_sinonimos_coincideConHorario() {
        when(preguntaFrecuenteDAO.findByActivaTrue()).thenReturn(List.of(faqHorario()));

        ChatbotRespuestaDTO resultado = service.responder("¿A qué hora abren?");

        assertEquals(RESPUESTA_HORARIO, resultado.getRespuesta());
        // 1 palabra en común: cobertura 1/2 y precisión 1/1 -> (0.5 + 1) / 2
        assertEquals(0.75, resultado.getCoincidencia());
    }

    @Test
    @DisplayName("responder: eligió la pregunta con mayor puntaje entre varias")
    void responder_variasPreguntas_eligeLaMejor() {
        when(preguntaFrecuenteDAO.findByActivaTrue()).thenReturn(List.of(faqPrestamo(), faqHorario()));

        ChatbotRespuestaDTO resultado = service.responder("horario de atencion");

        assertEquals(PREGUNTA_HORARIO, resultado.getPreguntaCoincidente());
    }

    @Test
    @DisplayName("responder: reemplaza las variables {DIAS_PRESTAMO} con la configuración vigente")
    void responder_respuestaConVariable_resuelveConfiguracion() {
        when(preguntaFrecuenteDAO.findByActivaTrue()).thenReturn(List.of(faqPrestamo()));
        when(configuracionService.plazo(ClavePlazo.DIAS_PRESTAMO)).thenReturn(14);

        ChatbotRespuestaDTO resultado = service.responder("cuantos dias dura el prestamo");

        assertEquals("El prestamo dura 14 dias", resultado.getRespuesta());
    }

    @Test
    @DisplayName("responder: variable desconocida se deja tal cual en la respuesta")
    void responder_variableDesconocida_seDejaTalCual() {
        PreguntaFrecuenteDTO conVariableRara = faq(3L, "Cual es el costo del carnet?", "Cuesta {VARIABLE_RARA}", null);
        when(preguntaFrecuenteDAO.findByActivaTrue()).thenReturn(List.of(conVariableRara));

        ChatbotRespuestaDTO resultado = service.responder("costo del carnet");

        assertEquals("Cuesta {VARIABLE_RARA}", resultado.getRespuesta());
    }

    // ---------------------------------------------------------------- sin coincidencia

    @Test
    @DisplayName("responder: sin coincidencia devuelve mensaje genérico, sin pregunta ni categoría")
    void responder_sinCoincidencia_devuelveMensajeGenerico() {
        when(preguntaFrecuenteDAO.findByActivaTrue()).thenReturn(List.of(faqHorario(), faqPrestamo()));

        ChatbotRespuestaDTO resultado = service.responder("zzz qwerty");

        assertTrue(resultado.getRespuesta().startsWith("No encontre una respuesta"));
        assertNull(resultado.getPreguntaCoincidente());
        assertNull(resultado.getCategoria());
        assertEquals(0.0, resultado.getCoincidencia());
    }

    @Test
    @DisplayName("responder: sin coincidencia sugiere como máximo 3 preguntas")
    void responder_sinCoincidencia_sugiereMaximoTres() {
        when(preguntaFrecuenteDAO.findByActivaTrue()).thenReturn(List.of(
                faq(1L, "Pregunta uno", "r1", null),
                faq(2L, "Pregunta dos", "r2", null),
                faq(3L, "Pregunta tres", "r3", null),
                faq(4L, "Pregunta cuatro", "r4", null)));

        ChatbotRespuestaDTO resultado = service.responder("zzz qwerty");

        assertEquals(3, resultado.getSugerencias().size());
    }

    @Test
    @DisplayName("responder: coincidencia por debajo del umbral no responde y sugiere la más cercana primero")
    void responder_debajoDelUmbral_noResponde_sugiereLaMasCercana() {
        PreguntaFrecuenteDTO horario = faq(1L, PREGUNTA_HORARIO, RESPUESTA_HORARIO, null);
        PreguntaFrecuenteDTO prestamo = faq(2L, "Cuantos dias dura el prestamo?", "r", null);
        // El orden de la lista es el contrario al del puntaje para comprobar que se ordena.
        when(preguntaFrecuenteDAO.findByActivaTrue()).thenReturn(List.of(prestamo, horario));

        // 1 palabra en común con 'horario': (1/2 + 1/6) / 2 = 0.33 < umbral 0.34
        ChatbotRespuestaDTO resultado = service.responder("horario prestamo multas sedes bibliotecas digitales");

        assertTrue(resultado.getCoincidencia() < ChatbotServiceImpl.UMBRAL_COINCIDENCIA);
        assertEquals(0.33, resultado.getCoincidencia());
        assertNull(resultado.getPreguntaCoincidente());
        assertEquals(List.of(PREGUNTA_HORARIO, "Cuantos dias dura el prestamo?"), resultado.getSugerencias());
    }

    @Test
    @DisplayName("responder: mensaje solo con palabras vacías ('hola') no coincide con nada")
    void responder_soloPalabrasVacias_noCoincide() {
        when(preguntaFrecuenteDAO.findByActivaTrue()).thenReturn(List.of(faqHorario()));

        ChatbotRespuestaDTO resultado = service.responder("hola, buenas tardes");

        assertNull(resultado.getPreguntaCoincidente());
        assertEquals(0.0, resultado.getCoincidencia());
    }

    @Test
    @DisplayName("responder: sin preguntas frecuentes activas devuelve mensaje genérico y sin sugerencias")
    void responder_baseVacia_sinSugerencias() {
        when(preguntaFrecuenteDAO.findByActivaTrue()).thenReturn(List.of());

        ChatbotRespuestaDTO resultado = service.responder("horario de atencion");

        assertTrue(resultado.getRespuesta().startsWith("No encontre una respuesta"));
        assertTrue(resultado.getSugerencias().isEmpty());
        assertEquals(0.0, resultado.getCoincidencia());
    }
}

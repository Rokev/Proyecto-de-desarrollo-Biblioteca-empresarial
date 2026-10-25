package bussinesLayer.config;

import bussinesLayer.dto.PreguntaFrecuenteDTO;
import bussinesLayer.service.PreguntaFrecuenteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Si la base de conocimiento esta vacia, carga unas preguntas basicas para que el chatbot responda desde el inicio.
 * Las respuestas usan variables ({DIAS_PRESTAMO}, {MULTA_MORA_POR_DIA}...) que el chatbot reemplaza con la
 * configuracion vigente ( 74/75/76). En bases ya cargadas, convierte las respuestas originales (con numeros
 * fijos) a su version con variables y agrega las preguntas nuevas; una respuesta editada a mano no se toca.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PreguntaFrecuenteInicializador implements ApplicationRunner {

    /** respuestaAnterior: texto original con numeros fijos, para migrar bases ya cargadas. nueva: se agrega si no existe. */
    private record Faq(String categoria, String pregunta, String respuesta, String respuestaAnterior, boolean nueva) {
        Faq(String categoria, String pregunta, String respuesta) {
            this(categoria, pregunta, respuesta, null, false);
        }
    }

    private static final List<Faq> INICIALES = List.of(
            new Faq("Prestamos", "Cuantos dias dura un prestamo de libro?",
                    "Un prestamo dura {DIAS_PRESTAMO} dias contados desde que recibes el libro. Si te pasas, se genera una multa de ${MULTA_MORA_POR_DIA} por cada dia de retraso.",
                    "Un prestamo dura 14 dias contados desde que recibes el libro. Si te pasas, se genera una multa por cada dia de retraso.",
                    false),
            new Faq("Prestamos", "Cuantos libros puedo tener prestados al mismo tiempo?",
                    "Puedes tener hasta 3 prestamos vigentes a la vez."),
            new Faq("Devoluciones", "Puedo devolver un libro en otra sede?",
                    "Si. Puedes devolverlo en cualquier sede en operacion; la sede lo regresa a su biblioteca de origen."),
            new Faq("Reservas", "Como reservo un libro que no esta disponible?",
                    "Reserva el libro en la sede que prefieras. Te avisamos cuando este disponible y tendras {HORAS_RETIRO_LIBRO} horas para retirarlo.",
                    "Reserva el libro en la sede que prefieras. Te avisamos cuando este disponible y tendras 2 dias para retirarlo.",
                    false),
            new Faq("Multas", "Que pasa si devuelvo un libro tarde?",
                    "Se genera una multa de ${MULTA_MORA_POR_DIA} por cada dia de retraso y {SANCION_MORA_DEVOLUCION}. Debes pagar la multa para volver a pedir libros.",
                    "Se genera una multa por cada dia de retraso y un bloqueo de prestamos hasta que devuelvas el libro y pagues la multa.",
                    false),
            new Faq("Multas", "Que pasa si un libro se dana o se pierde?",
                    "Se cobra una multa de ${MULTA_LIBRO_DANADO} si el libro se devuelve danado y de ${MULTA_LIBRO_PERDIDO} si se pierde. Danado: {SANCION_LIBRO_DANADO}. Perdido: {SANCION_LIBRO_PERDIDO}.",
                    null, true),
            new Faq("Pedidos interciudad", "Cuanto tiempo tengo para devolver un pedido interciudad?",
                    "Tienes {DIAS_PRESTAMO_INTERCIUDAD} dias desde que recibes el libro en tu casa (o lo retiras en la sede de devolucion). Debes devolverlo en la sede de devolucion que elegiste al hacer el pedido.",
                    null, true),
            new Faq("Pedidos interciudad", "Que pasa si no recibo o no recojo a tiempo mi pedido interciudad?",
                    "Si no recibes el pedido en la fecha y hora indicadas se cobra un recargo de ${MULTA_RECARGO_PEDIDO_NO_RECIBIDO} y el libro queda en la sede de devolucion, donde tienes {DIAS_RECOGIDA_SEDE_DEVOLUCION} dias para recogerlo. Si no lo recoges a tiempo se genera una multa de ${MULTA_PEDIDO_NO_RECOGIDO} y {SANCION_PEDIDO_NO_RECOGIDO}.",
                    null, true),
            new Faq("Multas", "Como pago una multa?",
                    "Desde la seccion de pagos puedes pagar tus multas con tarjeta. Al aprobarse el pago recuperas el acceso."),
            new Faq("Suscripcion", "Que beneficios tiene la suscripcion?",
                    "Con suscripcion puedes pedir libros de otras ciudades a domicilio, reservar eventos exclusivos y obtener descuento en salas de estudio."),
            new Faq("Suscripcion", "Como cancelo mi suscripcion?",
                    "Puedes cancelarla en cualquier momento; conservas los beneficios hasta que termine el periodo que ya pagaste."),
            new Faq("Pedidos interciudad", "Como funciona el pedido de un libro de otra ciudad?",
                    "Con suscripcion vigente eliges el libro, tu direccion, la fecha y hora para recibirlo y la sede donde lo devolveras. Pagas el envio y un repartidor lo lleva a tu casa."),
            new Faq("Salas de estudio", "Como reservo una sala de estudio?",
                    "Elige la sede, la sala, la fecha y la hora, y paga la reserva en los siguientes 30 minutos o se cancela automaticamente."),
            new Faq("Eventos", "Como me inscribo a un evento?",
                    "Entra al detalle del evento y reserva tus cupos. Los eventos exclusivos requieren una suscripcion vigente."),
            new Faq("Sedes", "Cual es el horario de atencion de las bibliotecas?",
                    "Cada sede tiene su propio horario; consultalo en la lista de bibliotecas."));

    private final PreguntaFrecuenteService preguntaFrecuenteService;

    @Override
    public void run(ApplicationArguments args) {
        List<PreguntaFrecuenteDTO> existentes = preguntaFrecuenteService.obtenerTodasLasPreguntasFrecuentes();
        if (!existentes.isEmpty()) {
            actualizarBaseExistente(existentes);
            return;
        }
        for (Faq faq : INICIALES) {
            preguntaFrecuenteService.crearPreguntaFrecuente(nuevaDto(faq));
        }
        log.info("Base de conocimiento del chatbot inicializada con {} preguntas", INICIALES.size());
    }

    private void actualizarBaseExistente(List<PreguntaFrecuenteDTO> existentes) {
        int migradas = 0;
        int agregadas = 0;
        for (Faq faq : INICIALES) {
            PreguntaFrecuenteDTO actual = existentes.stream()
                    .filter(e -> faq.pregunta().equals(e.getPregunta()))
                    .findFirst().orElse(null);
            if (actual == null) {
                if (faq.nueva()) {
                    preguntaFrecuenteService.crearPreguntaFrecuente(nuevaDto(faq));
                    agregadas++;
                }
            } else if (faq.respuestaAnterior() != null && faq.respuestaAnterior().equals(actual.getRespuesta())) {
                actual.setRespuesta(faq.respuesta());
                preguntaFrecuenteService.actualizarPreguntaFrecuente(actual.getId(), actual);
                migradas++;
            }
        }
        if (migradas > 0 || agregadas > 0) {
            log.info("Chatbot: {} respuestas ahora usan la configuracion vigente y {} preguntas nuevas agregadas", migradas, agregadas);
        }
    }

    private PreguntaFrecuenteDTO nuevaDto(Faq faq) {
        PreguntaFrecuenteDTO dto = new PreguntaFrecuenteDTO();
        dto.setCategoria(faq.categoria());
        dto.setPregunta(faq.pregunta());
        dto.setRespuesta(faq.respuesta());
        dto.setActiva(true);
        return dto;
    }
}

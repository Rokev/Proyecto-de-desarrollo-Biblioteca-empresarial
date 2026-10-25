package bussinesLayer.config;

import bussinesLayer.dto.PreguntaFrecuenteDTO;
import bussinesLayer.service.PreguntaFrecuenteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/** Si la base de conocimiento esta vacia, carga unas preguntas basicas para que el chatbot responda desde el inicio. */
@Component
@RequiredArgsConstructor
@Slf4j
public class PreguntaFrecuenteInicializador implements ApplicationRunner {

    private record Faq(String categoria, String pregunta, String respuesta) {
    }

    private static final List<Faq> INICIALES = List.of(
            new Faq("Prestamos", "Cuantos dias dura un prestamo de libro?",
                    "Un prestamo dura 14 dias contados desde que recibes el libro. Si te pasas, se genera una multa por cada dia de retraso."),
            new Faq("Prestamos", "Cuantos libros puedo tener prestados al mismo tiempo?",
                    "Puedes tener hasta 3 prestamos vigentes a la vez."),
            new Faq("Devoluciones", "Puedo devolver un libro en otra sede?",
                    "Si. Puedes devolverlo en cualquier sede en operacion; la sede lo regresa a su biblioteca de origen."),
            new Faq("Reservas", "Como reservo un libro que no esta disponible?",
                    "Reserva el libro en la sede que prefieras. Te avisamos cuando este disponible y tendras 2 dias para retirarlo."),
            new Faq("Multas", "Que pasa si devuelvo un libro tarde?",
                    "Se genera una multa por cada dia de retraso y un bloqueo de prestamos hasta que devuelvas el libro y pagues la multa."),
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
        if (!preguntaFrecuenteService.obtenerTodasLasPreguntasFrecuentes().isEmpty()) {
            return;
        }
        for (Faq faq : INICIALES) {
            PreguntaFrecuenteDTO dto = new PreguntaFrecuenteDTO();
            dto.setCategoria(faq.categoria());
            dto.setPregunta(faq.pregunta());
            dto.setRespuesta(faq.respuesta());
            dto.setActiva(true);
            preguntaFrecuenteService.crearPreguntaFrecuente(dto);
        }
        log.info("Base de conocimiento del chatbot inicializada con {} preguntas", INICIALES.size());
    }
}

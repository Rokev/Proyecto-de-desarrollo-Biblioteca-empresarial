package bussinesLayer.service;
import java.util.Optional;
import java.time.LocalDate;

import bussinesLayer.dto.EventoDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de eventos de las sedes.
 */
public interface EventoService {

    EventoDTO crearEvento(EventoDTO eventoDTO);

    EventoDTO obtenerEventoPorId(Long id);

    List<EventoDTO> obtenerTodosLosEventos();

    EventoDTO actualizarEvento(Long id, EventoDTO eventoDTO);

    void eliminarEvento(Long id);

    /**  17: eventos proximos no cancelados, filtrables por sede, fecha y tipo (exclusivo o general). */
    List<EventoDTO> buscarEventosProximos(Long sedeId, LocalDate fecha, Boolean exclusivo);

    /**
     *  47: sin inscripciones el evento se elimina (Optional vacio, sin notificar); con inscripciones se liberan
     * los cupos, se notifica a los asistentes y el evento queda CANCELADO.
     */
    Optional<EventoDTO> cancelarEvento(Long id);
}

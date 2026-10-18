package bussinesLayer.service;

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
}

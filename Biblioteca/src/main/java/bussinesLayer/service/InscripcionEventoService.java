package bussinesLayer.service;

import bussinesLayer.dto.InscripcionEventoDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de inscripciones de usuarios a eventos.
 */
public interface InscripcionEventoService {

    InscripcionEventoDTO crearInscripcionEvento(InscripcionEventoDTO inscripcionEventoDTO);

    InscripcionEventoDTO obtenerInscripcionEventoPorId(Long id);

    List<InscripcionEventoDTO> obtenerTodasLasInscripcionesEvento();

    InscripcionEventoDTO actualizarInscripcionEvento(Long id, InscripcionEventoDTO inscripcionEventoDTO);

    void eliminarInscripcionEvento(Long id);
}

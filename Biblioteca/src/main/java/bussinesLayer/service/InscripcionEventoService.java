package bussinesLayer.service;
import bussinesLayer.dto.AsistenteEventoDTO;

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
    
    /** Cancela las inscripciones a eventos exclusivos futuros de usuarios sin suscripcion vigente
     *  y libera sus cupos. Devuelve cuantas cancelo. */
    int cancelarInscripcionesExclusivasSinSuscripcion();

    void eliminarInscripcionEvento(Long id);

    /** asistentes de un evento; estado null devuelve los ACTIVOS, TODAS devuelve todos. */
    List<AsistenteEventoDTO> obtenerAsistentesDeEvento(Long eventoId, String estado);
}

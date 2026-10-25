package bussinesLayer.service;

import bussinesLayer.dto.TrasladoRetornoDTO;

import java.util.List;

/**
 * Regreso de los ejemplares a su sede de origen tras una devolucion en otra sede.
 */
public interface TrasladoRetornoService {

    TrasladoRetornoDTO obtenerTrasladoPorId(Long id);

    /** Traslados que vienen hacia una sede (la de origen). Si estado es null o vacio, trae todos. */
    List<TrasladoRetornoDTO> obtenerTrasladosHaciaSede(Long sedeDestinoId, String estado);

    /** La sede de origen recibe el ejemplar: queda disponible y se atiende la reserva en espera. */
    TrasladoRetornoDTO recibirTraslado(Long trasladoId, Long bibliotecarioId);
}
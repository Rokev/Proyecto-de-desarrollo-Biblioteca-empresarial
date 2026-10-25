package bussinesLayer.service;
import java.time.LocalDateTime;
import bussinesLayer.dto.DisponibilidadSalasDTO;

import bussinesLayer.dto.SalaEstudioDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de salas de estudio.
 */
public interface SalaEstudioService {

    SalaEstudioDTO crearSalaEstudio(SalaEstudioDTO salaEstudioDTO);

    SalaEstudioDTO obtenerSalaEstudioPorId(Long id);

    List<SalaEstudioDTO> obtenerTodasLasSalasEstudio();

    SalaEstudioDTO actualizarSalaEstudio(Long id, SalaEstudioDTO salaEstudioDTO);

    void eliminarSalaEstudio(Long id);

    /**si la sede tiene salas, cuantas estan libres y el estado de cada una en el rango. */
    DisponibilidadSalasDTO consultarDisponibilidadDeSede(Long sedeId, LocalDateTime inicio, LocalDateTime fin);
}

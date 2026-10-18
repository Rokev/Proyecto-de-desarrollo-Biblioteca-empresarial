package bussinesLayer.service;

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
}

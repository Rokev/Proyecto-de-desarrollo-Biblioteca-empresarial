package bussinesLayer.service;

import bussinesLayer.dto.SedeDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de sedes de la red de bibliotecas.
 */
public interface SedeService {

    SedeDTO crearSede(SedeDTO sedeDTO);

    SedeDTO obtenerSedePorId(Long id);

    List<SedeDTO> obtenerTodasLasSedes();

    SedeDTO actualizarSede(Long id, SedeDTO sedeDTO);

    void eliminarSede(Long id);

    /**solo las sedes en operacion. */
    List<SedeDTO> obtenerSedesActivas();
}

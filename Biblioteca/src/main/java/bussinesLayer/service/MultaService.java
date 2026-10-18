package bussinesLayer.service;

import bussinesLayer.dto.MultaDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de multas de los usuarios.
 */
public interface MultaService {

    MultaDTO crearMulta(MultaDTO multaDTO);

    MultaDTO obtenerMultaPorId(Long id);

    List<MultaDTO> obtenerTodasLasMultas();

    MultaDTO actualizarMulta(Long id, MultaDTO multaDTO);

    void eliminarMulta(Long id);
}

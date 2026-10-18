package bussinesLayer.service;

import bussinesLayer.dto.EjemplarDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de ejemplares (copias fisicas) de los libros.
 */
public interface EjemplarService {

    EjemplarDTO crearEjemplar(EjemplarDTO ejemplarDTO);

    EjemplarDTO obtenerEjemplarPorId(Long id);

    List<EjemplarDTO> obtenerTodosLosEjemplares();

    EjemplarDTO actualizarEjemplar(Long id, EjemplarDTO ejemplarDTO);

    void eliminarEjemplar(Long id);
}

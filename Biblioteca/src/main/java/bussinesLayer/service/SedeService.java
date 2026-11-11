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

    /** solo las sedes en operacion. */
    List<SedeDTO> obtenerSedesActivas();

    /** pone la sede fuera de servicio solo si no tiene prestamos activos, reservas pendientes ni pedidos interciudad en curso. */
    SedeDTO desactivarSede(Long id, Long administradorId);

    /** devuelve la sede a servicio. */
    SedeDTO reactivarSede(Long id);
}

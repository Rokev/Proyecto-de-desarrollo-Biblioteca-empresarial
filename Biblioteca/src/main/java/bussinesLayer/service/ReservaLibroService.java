package bussinesLayer.service;
import bussinesLayer.dto.ReservaLibroDetalleDTO;

import bussinesLayer.dto.ReservaLibroDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de reservas de libros sin stock.
 */
public interface ReservaLibroService {

    ReservaLibroDTO crearReservaLibro(ReservaLibroDTO reservaLibroDTO);

    ReservaLibroDTO obtenerReservaLibroPorId(Long id);

    List<ReservaLibroDTO> obtenerTodasLasReservasLibro();

    ReservaLibroDTO actualizarReservaLibro(Long id, ReservaLibroDTO reservaLibroDTO);

    void eliminarReservaLibro(Long id);

    /** Avisa al primer usuario en espera (libro y sede) que su libro fue devuelto y va de regreso a esa sede. */
    void avisarDevolucionEnCamino(Long libroId, Long sedeOrigenId, String nombreSedeActual);

    /** Si hay una reserva EN_ESPERA para este ejemplar (libro y sede), se lo aparta, la pasa a NOTIFICADA
     *  y avisa al usuario. Devuelve true si el ejemplar quedo asignado a una reserva. */
    boolean asignarEjemplarAReservaEnEspera(Long ejemplarId);
        

    /** reservas del usuario (libro, sede, estado y fecha limite de retiro). */
    List<ReservaLibroDetalleDTO> obtenerReservasDeUsuario(Long usuarioId, String estado);

    /** reservas de la sede, filtrables por estado, usuario y libro. */
    List<ReservaLibroDetalleDTO> obtenerReservasDeSede(Long sedeId, String estado, Long usuarioId, Long libroId);
}

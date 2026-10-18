package bussinesLayer.service;

import bussinesLayer.dto.ReservaSalaDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de reservas de salas de estudio.
 */
public interface ReservaSalaService {

    ReservaSalaDTO crearReservaSala(ReservaSalaDTO reservaSalaDTO);

    ReservaSalaDTO obtenerReservaSalaPorId(Long id);

    List<ReservaSalaDTO> obtenerTodasLasReservasSala();

    ReservaSalaDTO actualizarReservaSala(Long id, ReservaSalaDTO reservaSalaDTO);

    void eliminarReservaSala(Long id);
}

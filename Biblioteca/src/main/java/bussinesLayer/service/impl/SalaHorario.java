package bussinesLayer.service.impl;

import bussinesLayer.dto.SalaEstudioDTO;

import java.time.LocalDateTime;

/** Regla de horario de atencion de una sala (compartida por disponibilidad y reservas). */
final class SalaHorario {

    private SalaHorario() {
    }

    /** True si la sala no tiene horario, o si el rango cae completo dentro de su horario en un mismo dia. */
    static boolean dentroDeHorario(SalaEstudioDTO sala, LocalDateTime inicio, LocalDateTime fin) {
        if (sala.getHoraApertura() == null || sala.getHoraCierre() == null) {
            return true;
        }
        return inicio.toLocalDate().equals(fin.toLocalDate())
                && !inicio.toLocalTime().isBefore(sala.getHoraApertura())
                && !fin.toLocalTime().isAfter(sala.getHoraCierre());
    }

    static String descripcion(SalaEstudioDTO sala) {
        return sala.getHoraApertura() + " a " + sala.getHoraCierre();
    }
}
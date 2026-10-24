package bussinesLayer.service;

/**
 * Procesos automaticos (RF-39, RF-75, RF-76) que dependen del paso del tiempo.
 * Los invoca {@code TareasProgramadas}; cada metodo devuelve cuantos registros proceso.
 */
public interface AutomatizacionService {

    /** Seccion 1: prestamos ACTIVO con fechaVencimiento pasada -> VENCIDO + multa por mora + notificacion. */
    int procesarPrestamosVencidos();

    /** Seccion 2a: solicitudes de prestamo en sede (SOLICITADO) que no se retiraron a tiempo -> CANCELADO. */
    int cancelarSolicitudesNoRetiradas();

    /** Seccion 2b: reservas NOTIFICADA cuyo plazo de retiro vencio -> EXPIRADA, y el ejemplar pasa al siguiente en espera. */
    int expirarReservasNoRetiradas();

    /** Multa por mora creciente: recalcula cada dia la multa de los prestamos VENCIDO (dias de retraso x tarifa diaria). */
    int actualizarMultasPorMora();

    /** Levanta (FINALIZADA) el bloqueo de prestamos por retraso cuando el libro ya fue devuelto. */
    int levantarSancionesPorDevolucion();

    /** Libera las salas: cancela reservas sin pago a tiempo y finaliza las confirmadas que ya terminaron. */
    int liberarReservasSalaVencidas();
}

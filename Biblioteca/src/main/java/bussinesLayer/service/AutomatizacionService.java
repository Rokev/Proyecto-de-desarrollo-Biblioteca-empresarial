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

    /** Recordatorios de préstamos próximos a vencer (24 h y 48 h antes). */
    int enviarRecordatoriosPrestamosProximos();

    /** Recordatorios de eventos programados que inician en las próximas 24 horas. */
    int enviarRecordatoriosEventosProximos();

    /** Recordatorios de reservas de salas de estudio que inician en las próximas 2 horas. */
    int enviarRecordatoriosSalasProximas();
    
    /** Cancela los pedidos interciudad PENDIENTE_PAGO que superaron el plazo de pago y libera el ejemplar. */
    int cancelarPedidosInterciudadSinPago();

    /** Pedidos interciudad EN_ESPERA_DE_RECOGIDA con plazo vencido: multa, sanción y regreso a la sede de origen. */
    int procesarPedidosNoRecogidos();
    
    /** Sanciones ACTIVA con fechaFin pasada -> FINALIZADA (el usuario recupera el acceso). */
    int finalizarSancionesVencidas();

    /**
     * Se invoca al devolver un préstamo (normal o interciudad): si llegó tarde, deja aplicadas la multa
     * por mora y la sanción. Devuelve los días de retraso (0 si fue a tiempo).
     */
    long evaluarRetrasoAlDevolver(Long prestamoId);
}

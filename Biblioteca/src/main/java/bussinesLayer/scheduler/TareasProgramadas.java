package bussinesLayer.scheduler;

import bussinesLayer.service.AutomatizacionService;
import bussinesLayer.service.InscripcionEventoService;
import bussinesLayer.service.SuscripcionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Mantenimiento diario y envío de recordatorios automáticos. */
@Component
@RequiredArgsConstructor
@Slf4j
public class TareasProgramadas {

    private final SuscripcionService suscripcionService;
    private final InscripcionEventoService inscripcionEventoService;
    private final AutomatizacionService automatizacionService;

    /** Todos los dias a las 00:05 (hora de Bogota). */
    @Scheduled(cron = "0 5 0 * * *", zone = "America/Bogota")
    public void mantenimientoDiario() {
        try {
            suscripcionService.marcarSuscripcionesVencidas();
        } catch (Exception e) {
            log.error("Error al marcar suscripciones vencidas", e);
        }
        try {
            inscripcionEventoService.cancelarInscripcionesExclusivasSinSuscripcion();
        } catch (Exception e) {
            log.error("Error al cancelar inscripciones a eventos exclusivos", e);
        }
        try {
            automatizacionService.procesarPrestamosVencidos();
        } catch (Exception e) {
            log.error("Error al procesar prestamos vencidos", e);
        }
        try {
            automatizacionService.actualizarMultasPorMora();
        } catch (Exception e) {
            log.error("Error al actualizar multas por mora", e);
        }
        try {
            automatizacionService.enviarRecordatoriosPrestamosProximos();
        } catch (Exception e) {
            log.error("Error al enviar recordatorios de prestamos proximos", e);
        }
        try {
            automatizacionService.enviarRecordatoriosEventosProximos();
        } catch (Exception e) {
            log.error("Error al enviar recordatorios de eventos proximos", e);
        }
    }

    /**
     * Cada hora (minuto 10): los plazos de retiro se miden en horas.
     */
    @Scheduled(cron = "0 10 * * * *", zone = "America/Bogota")
    public void expirarPlazosDeRetiro() {
        try {
            automatizacionService.cancelarSolicitudesNoRetiradas();
        } catch (Exception e) {
            log.error("Error al cancelar solicitudes de prestamo no retiradas", e);
        }
        try {
            automatizacionService.expirarReservasNoRetiradas();
        } catch (Exception e) {
            log.error("Error al expirar reservas no retiradas", e);
        }
        try {
            automatizacionService.levantarSancionesPorDevolucion();
        } catch (Exception e) {
            log.error("Error al levantar sanciones por devolucion", e);
        }
        try {
            automatizacionService.enviarRecordatoriosSalasProximas();
        } catch (Exception e) {
            log.error("Error al enviar recordatorios de reservas de salas", e);
        }
    }

    /** Cada 10 minutos: el plazo para pagar una reserva de sala es corto (30 min). */
    @Scheduled(cron = "0 */10 * * * *", zone = "America/Bogota")
    public void liberarSalas() {
        try {
            automatizacionService.liberarReservasSalaVencidas();
        } catch (Exception e) {
            log.error("Error al liberar reservas de sala", e);
        }
    }
}
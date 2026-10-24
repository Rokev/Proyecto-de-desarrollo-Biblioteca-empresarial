package bussinesLayer.scheduler;

import bussinesLayer.service.InscripcionEventoService;
import bussinesLayer.service.SuscripcionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Mantenimiento diario: deja ordenados los estados que dependen del paso del tiempo. */
@Component
@RequiredArgsConstructor
@Slf4j
public class TareasProgramadas {

    private final SuscripcionService suscripcionService;
    private final InscripcionEventoService inscripcionEventoService;

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
    }
}
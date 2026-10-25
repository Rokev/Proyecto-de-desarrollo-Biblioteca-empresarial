package bussinesLayer.service;

import bussinesLayer.dto.CompraSuscripcionDTO;
import bussinesLayer.dto.SuscripcionDTO;
import java.util.Optional;

import java.util.List;

/**
 * Logica de negocio para la gestion de suscripciones de usuarios.
 */
public interface SuscripcionService {

    SuscripcionDTO crearSuscripcion(SuscripcionDTO suscripcionDTO);

    SuscripcionDTO obtenerSuscripcionPorId(Long id);

    List<SuscripcionDTO> obtenerTodasLasSuscripciones();

    SuscripcionDTO actualizarSuscripcion(Long id, SuscripcionDTO suscripcionDTO);

    void eliminarSuscripcion(Long id);

    /**crea la suscripcion, registra el pago y activa los beneficios en un solo paso. */
    SuscripcionDTO comprarSuscripcion(CompraSuscripcionDTO compra);

    /** Activa una suscripcion PENDIENTE_PAGO (la usa la compra y los pagos exitosos). */
    SuscripcionDTO activarSuscripcion(Long suscripcionId);

    /** cancela; los beneficios se conservan hasta fechaFin. */
    SuscripcionDTO cancelarSuscripcion(Long suscripcionId, Long usuarioId);

    /** devuelve la suscripcion vigente del usuario o lanza 404 si no tiene. */
    SuscripcionDTO obtenerSuscripcionVigente(Long usuarioId);

    /** true si el usuario tiene una suscripcion vigente hoy. */
    boolean tieneSuscripcionVigente(Long usuarioId);

    /** lanza 400 si el usuario no tiene suscripcion vigente. */
    void exigirSuscripcionVigente(Long usuarioId);
    
    /** la suscripcion vigente del usuario, o vacio si no tiene (no lanza excepcion). */
    Optional<SuscripcionDTO> buscarSuscripcionVigente(Long usuarioId);

    /** Marca como VENCIDA las ACTIVA cuya fechaFin ya paso. Devuelve cuantas marco. */
    int marcarSuscripcionesVencidas();
}
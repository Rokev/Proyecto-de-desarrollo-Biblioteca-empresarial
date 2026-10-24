package bussinesLayer.service;

import bussinesLayer.dto.PedidoInterciudadDTO;
import bussinesLayer.dto.SolicitudPedidoInterciudadDTO;

import java.util.List;

/**
 * Logica de negocio de los pedidos interciudad.
 */
public interface PedidoInterciudadService {

    /** exige suscripcion vigente y reserva un ejemplar de otra ciudad. */
    PedidoInterciudadDTO solicitarPedido(SolicitudPedidoInterciudadDTO solicitud);

    PedidoInterciudadDTO obtenerPedidoPorId(Long id);

    /**pedidos del usuario; si estado es null o vacio, trae todos. */
    List<PedidoInterciudadDTO> obtenerPedidosDeUsuario(Long usuarioId, String estado);

    /** Para el bibliotecario: pedidos en un estado dado (los mas antiguos primero). */
    List<PedidoInterciudadDTO> obtenerPedidosPorEstado(String estado);

    /**solo en estado SOLICITADO; en transito se bloquea y se notifica al usuario. */
    PedidoInterciudadDTO cancelarPedido(Long pedidoId, Long usuarioId);

    PedidoInterciudadDTO marcarEnTransito(Long pedidoId, Long bibliotecarioId);

    PedidoInterciudadDTO marcarDisponibleParaRetiro(Long pedidoId, Long bibliotecarioId);

    /** Registra la entrega: activa el prestamo y empieza a correr el plazo de devolucion. */
    PedidoInterciudadDTO marcarRetirado(Long pedidoId, Long bibliotecarioId);
}
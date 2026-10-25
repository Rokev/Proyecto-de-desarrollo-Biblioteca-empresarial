package bussinesLayer.service;

import bussinesLayer.dto.PedidoInterciudadDTO;
import bussinesLayer.dto.SolicitudPedidoInterciudadDTO;
import java.util.List;

public interface PedidoInterciudadService {

    // --- ROL USUARIO CON SUSCRIPCIÓN ---
    PedidoInterciudadDTO solicitarPedido(SolicitudPedidoInterciudadDTO solicitud);
    PedidoInterciudadDTO cancelarPedido(Long pedidoId, Long usuarioId); 
    PedidoInterciudadDTO obtenerPedidoPorId(Long id);
    List<PedidoInterciudadDTO> obtenerPedidosDeUsuario(Long usuarioId, String estado); 

    // --- ROL BIBLIOTECARIO ---
    List<PedidoInterciudadDTO> obtenerPedidosSedeOrigen(Long sedeOrigenId); 
    PedidoInterciudadDTO despacharPedido(Long pedidoId, Long bibliotecarioId, Long repartidorId); 
    PedidoInterciudadDTO registrarDevolucionEnSede(Long pedidoId, Long bibliotecarioId, Long sedeId); 
    PedidoInterciudadDTO registrarRecepcionNoRecibido(Long pedidoId, Long bibliotecarioId); 
    PedidoInterciudadDTO registrarEntregaEnSede(Long pedidoId, Long bibliotecarioId); 
    PedidoInterciudadDTO registrarRecepcionEnSedeOrigen(Long pedidoId, Long bibliotecarioId); 
    List<PedidoInterciudadDTO> obtenerPedidosSedeDevolucion(Long sedeDevolucionId);

    // --- ROL REPARTIDOR ---
    List<PedidoInterciudadDTO> obtenerAsignacionesRepartidor(Long repartidorId);
    PedidoInterciudadDTO confirmarEntregaDomicilio(Long pedidoId, Long repartidorId);
    PedidoInterciudadDTO registrarUsuarioNoRecibio(Long pedidoId, Long repartidorId); 
    PedidoInterciudadDTO iniciarTrasladoRetorno(Long pedidoId, Long repartidorId); 
}
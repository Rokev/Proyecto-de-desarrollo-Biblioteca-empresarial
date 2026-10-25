package bussinesLayer.service;

import bussinesLayer.dto.ComprobantePagoDTO;
import bussinesLayer.dto.PagoDTO;
import bussinesLayer.dto.SolicitudPagoDTO;

import java.util.List;

public interface PagoService {

    // --- MÉTODOS CRUD EXISTENTES ---
    PagoDTO crearPago(PagoDTO pagoDTO);

    PagoDTO obtenerPagoPorId(Long id);
    
    List<PagoDTO> obtenerTodosLosPagos();
    
    PagoDTO actualizarPago(Long id, PagoDTO pagoDTO);
    
    void eliminarPago(Long id);

    // --- MÉTODOS DE PASARELA DE PAGO SIMULADO ---
    ComprobantePagoDTO procesarPagoSimulado(SolicitudPagoDTO solicitud);
    
    ComprobantePagoDTO obtenerComprobantePorId(Long id);
    
    List<ComprobantePagoDTO> obtenerPagosPorUsuario(Long usuarioId);
}
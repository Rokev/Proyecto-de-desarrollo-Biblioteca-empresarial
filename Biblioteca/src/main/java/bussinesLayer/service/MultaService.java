package bussinesLayer.service;

import bussinesLayer.dto.MultaDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lógica de negocio para la gestión de multas y recargos de usuarios.
 */
public interface MultaService {

    MultaDTO crearMulta(MultaDTO multaDTO);

    MultaDTO obtenerMultaPorId(Long id);

    List<MultaDTO> obtenerTodasLasMultas();

    MultaDTO actualizarMulta(Long id, MultaDTO multaDTO);

    void eliminarMulta(Long id);

    List<MultaDTO> obtenerMultasPorUsuario(Long usuarioId, String estado);

    BigDecimal obtenerSaldoPendienteUsuario(Long usuarioId);

    MultaDTO generarMultaPorDanioOPerdida(Long usuarioId, Long prestamoId, BigDecimal monto, String motivo);
}
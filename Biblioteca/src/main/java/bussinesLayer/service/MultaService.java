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

    /**
     * Multa por libro dañado o perdido ( 41). tipo = DANADO o PERDIDO. Si monto es nulo se usa el configurado
     * ( 75) y, si la politica de sancion de esa infraccion esta activa, se aplica tambien la sancion ( 74).
     */
    MultaDTO generarMultaPorDanioOPerdida(Long usuarioId, Long prestamoId, String tipo, BigDecimal monto, String motivo);
}

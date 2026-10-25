package bussinesLayer.service;

import bussinesLayer.dto.AuditoriaDTO;
import persistanceLayer.dao.AuditoriaDAO;

import java.time.LocalDate;

/**
 * Lógica de negocio del log de auditoría ( 79 y el registro de acciones de  59,  63,  65,  77...).
 */
public interface AuditoriaService {

    /**
     * Registra una acción en el log. Los demás servicios llaman a este método después de guardar
     * el cambio. usuarioId puede ser nulo (acción del sistema).
     */
    AuditoriaDTO registrar(Long usuarioId, String accion, String modulo, Long entidadId, String descripcion);

    AuditoriaDTO obtenerPorId(Long id);

    /**
     * Consulta el historial filtrando por usuario, rango de fechas (ambas inclusivas), tipo de acción y módulo.
     * Todos los filtros son opcionales.
     */
    AuditoriaDAO.Pagina consultar(Long usuarioId, LocalDate desde, LocalDate hasta,
                                String accion, String modulo, int pagina, int tamanio);
}

package bussinesLayer.service;

import bussinesLayer.dto.JustificacionRetrasoDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de justificaciones de retraso en devoluciones.
 */
public interface JustificacionRetrasoService {

    JustificacionRetrasoDTO crearJustificacionRetraso(JustificacionRetrasoDTO justificacionRetrasoDTO);

    JustificacionRetrasoDTO obtenerJustificacionRetrasoPorId(Long id);

    List<JustificacionRetrasoDTO> obtenerTodasLasJustificacionesRetraso();

    JustificacionRetrasoDTO actualizarJustificacionRetraso(Long id, JustificacionRetrasoDTO justificacionRetrasoDTO);

    void eliminarJustificacionRetraso(Long id);
}

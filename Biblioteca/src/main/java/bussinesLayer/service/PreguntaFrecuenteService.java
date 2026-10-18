package bussinesLayer.service;

import bussinesLayer.dto.PreguntaFrecuenteDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de preguntas frecuentes del chatbot.
 */
public interface PreguntaFrecuenteService {

    PreguntaFrecuenteDTO crearPreguntaFrecuente(PreguntaFrecuenteDTO preguntaFrecuenteDTO);

    PreguntaFrecuenteDTO obtenerPreguntaFrecuentePorId(Long id);

    List<PreguntaFrecuenteDTO> obtenerTodasLasPreguntasFrecuentes();

    PreguntaFrecuenteDTO actualizarPreguntaFrecuente(Long id, PreguntaFrecuenteDTO preguntaFrecuenteDTO);

    void eliminarPreguntaFrecuente(Long id);
}

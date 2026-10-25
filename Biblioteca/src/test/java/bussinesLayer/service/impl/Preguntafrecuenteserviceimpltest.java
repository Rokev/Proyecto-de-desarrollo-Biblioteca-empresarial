package bussinesLayer.service.impl;

import bussinesLayer.dto.PreguntaFrecuenteDTO;
import bussinesLayer.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.PreguntaFrecuenteDAO;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PreguntaFrecuenteServiceImpl")
class PreguntaFrecuenteServiceImplTest {

    private static final Long PREGUNTA_ID = 1L;

    @Mock private PreguntaFrecuenteDAO preguntaFrecuenteDAO;

    @InjectMocks
    private PreguntaFrecuenteServiceImpl service;

    private PreguntaFrecuenteDTO dto;

    @BeforeEach
    void setUp() {
        dto = new PreguntaFrecuenteDTO();
        dto.setPregunta("Como renuevo un prestamo?");
        dto.setRespuesta("Desde la seccion Mis prestamos.");
        dto.setCategoria("PRESTAMOS");
    }

    private PreguntaFrecuenteDTO preguntaConId(Long id) {
        PreguntaFrecuenteDTO p = new PreguntaFrecuenteDTO();
        p.setId(id);
        return p;
    }

    // ---------------------------------------------------------------- crearPreguntaFrecuente

    @Test
    @DisplayName("crearPreguntaFrecuente: asigna activa=true cuando viene vacia")
    void crearPreguntaFrecuente_valida_aplicaActivaPorDefecto() {
        when(preguntaFrecuenteDAO.save(dto)).thenAnswer(inv -> {
            PreguntaFrecuenteDTO p = inv.getArgument(0);
            p.setId(PREGUNTA_ID);
            return p;
        });

        PreguntaFrecuenteDTO resultado = service.crearPreguntaFrecuente(dto);

        assertEquals(PREGUNTA_ID, resultado.getId());
        assertTrue(resultado.getActiva());
    }

    @Test
    @DisplayName("crearPreguntaFrecuente: conserva activa=false si ya viene informada")
    void crearPreguntaFrecuente_conActivaFalse_laConserva() {
        dto.setActiva(false);
        when(preguntaFrecuenteDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        PreguntaFrecuenteDTO resultado = service.crearPreguntaFrecuente(dto);

        assertFalse(resultado.getActiva());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerPreguntaFrecuentePorId: devuelve la pregunta cuando existe")
    void obtenerPreguntaFrecuentePorId_existente_laDevuelve() {
        PreguntaFrecuenteDTO existente = preguntaConId(PREGUNTA_ID);
        when(preguntaFrecuenteDAO.findById(PREGUNTA_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerPreguntaFrecuentePorId(PREGUNTA_ID));
    }

    @Test
    @DisplayName("obtenerPreguntaFrecuentePorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerPreguntaFrecuentePorId_inexistente_lanzaNotFound() {
        when(preguntaFrecuenteDAO.findById(PREGUNTA_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.obtenerPreguntaFrecuentePorId(PREGUNTA_ID));
    }

    @Test
    @DisplayName("obtenerTodasLasPreguntasFrecuentes: delega en el DAO")
    void obtenerTodasLasPreguntasFrecuentes_devuelveLista() {
        when(preguntaFrecuenteDAO.findAll()).thenReturn(List.of(preguntaConId(1L), preguntaConId(2L)));

        assertEquals(2, service.obtenerTodasLasPreguntasFrecuentes().size());
    }

    // ---------------------------------------------------------------- actualizarPreguntaFrecuente

    @Test
    @DisplayName("actualizarPreguntaFrecuente: devuelve la pregunta actualizada por el DAO")
    void actualizarPreguntaFrecuente_existente_actualiza() {
        when(preguntaFrecuenteDAO.update(PREGUNTA_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarPreguntaFrecuente(PREGUNTA_ID, dto));
    }

    @Test
    @DisplayName("actualizarPreguntaFrecuente: si el DAO no la encuentra lanza ResourceNotFoundException")
    void actualizarPreguntaFrecuente_inexistente_lanzaNotFound() {
        when(preguntaFrecuenteDAO.update(PREGUNTA_ID, dto)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.actualizarPreguntaFrecuente(PREGUNTA_ID, dto));
    }

    // ---------------------------------------------------------------- eliminarPreguntaFrecuente

    @Test
    @DisplayName("eliminarPreguntaFrecuente: elimina cuando la pregunta existe")
    void eliminarPreguntaFrecuente_existente_elimina() {
        when(preguntaFrecuenteDAO.deleteById(PREGUNTA_ID)).thenReturn(true);

        service.eliminarPreguntaFrecuente(PREGUNTA_ID);

        verify(preguntaFrecuenteDAO).deleteById(PREGUNTA_ID);
    }

    @Test
    @DisplayName("eliminarPreguntaFrecuente: pregunta inexistente lanza ResourceNotFoundException")
    void eliminarPreguntaFrecuente_inexistente_lanzaNotFound() {
        when(preguntaFrecuenteDAO.deleteById(PREGUNTA_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> service.eliminarPreguntaFrecuente(PREGUNTA_ID));
    }
}

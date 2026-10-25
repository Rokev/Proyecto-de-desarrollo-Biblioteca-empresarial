package bussinesLayer.service.impl;

import bussinesLayer.dto.PlanSuscripcionDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.PlanSuscripcionDAO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlanSuscripcionServiceImpl")
class PlanSuscripcionServiceImplTest {

    private static final Long PLAN_ID = 1L;
    private static final Long OTRO_ID = 2L;

    @Mock private PlanSuscripcionDAO planSuscripcionDAO;

    @InjectMocks
    private PlanSuscripcionServiceImpl service;

    private PlanSuscripcionDTO dto;

    @BeforeEach
    void setUp() {
        dto = new PlanSuscripcionDTO();
        dto.setNombre("Premium");
        dto.setDescripcion("Plan premium de la biblioteca");
        dto.setPrecio(new BigDecimal("50000"));
        dto.setDuracionMeses(12);
    }

    private PlanSuscripcionDTO planConId(Long id) {
        PlanSuscripcionDTO p = new PlanSuscripcionDTO();
        p.setId(id);
        return p;
    }

    // ---------------------------------------------------------------- crearPlanSuscripcion

    @Test
    @DisplayName("crearPlanSuscripcion: asigna activo=true cuando viene vacio")
    void crearPlanSuscripcion_valido_aplicaActivoPorDefecto() {
        when(planSuscripcionDAO.existsByNombreIgnoreCase("Premium")).thenReturn(false);
        when(planSuscripcionDAO.save(dto)).thenAnswer(inv -> {
            PlanSuscripcionDTO p = inv.getArgument(0);
            p.setId(PLAN_ID);
            return p;
        });

        PlanSuscripcionDTO resultado = service.crearPlanSuscripcion(dto);

        assertEquals(PLAN_ID, resultado.getId());
        assertTrue(resultado.getActivo());
    }

    @Test
    @DisplayName("crearPlanSuscripcion: conserva activo=false si ya viene informado")
    void crearPlanSuscripcion_conActivoFalse_loConserva() {
        dto.setActivo(false);
        when(planSuscripcionDAO.existsByNombreIgnoreCase("Premium")).thenReturn(false);
        when(planSuscripcionDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        PlanSuscripcionDTO resultado = service.crearPlanSuscripcion(dto);

        assertFalse(resultado.getActivo());
    }

    @Test
    @DisplayName("crearPlanSuscripcion: nombre duplicado lanza BusinessValidationException")
    void crearPlanSuscripcion_nombreDuplicado_lanzaBusinessValidation() {
        when(planSuscripcionDAO.existsByNombreIgnoreCase("Premium")).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearPlanSuscripcion(dto));

        verify(planSuscripcionDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerPlanSuscripcionPorId: devuelve el plan cuando existe")
    void obtenerPlanSuscripcionPorId_existente_loDevuelve() {
        PlanSuscripcionDTO existente = planConId(PLAN_ID);
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerPlanSuscripcionPorId(PLAN_ID));
    }

    @Test
    @DisplayName("obtenerPlanSuscripcionPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerPlanSuscripcionPorId_inexistente_lanzaNotFound() {
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerPlanSuscripcionPorId(PLAN_ID));
    }

    @Test
    @DisplayName("obtenerTodosLosPlanesSuscripcion: delega en el DAO")
    void obtenerTodosLosPlanesSuscripcion_devuelveLista() {
        when(planSuscripcionDAO.findAll()).thenReturn(List.of(planConId(1L), planConId(2L)));

        assertEquals(2, service.obtenerTodosLosPlanesSuscripcion().size());
    }

    // ---------------------------------------------------------------- actualizarPlanSuscripcion

    @Test
    @DisplayName("actualizarPlanSuscripcion: plan inexistente lanza ResourceNotFoundException")
    void actualizarPlanSuscripcion_inexistente_lanzaNotFound() {
        when(planSuscripcionDAO.existsById(PLAN_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarPlanSuscripcion(PLAN_ID, dto));

        verify(planSuscripcionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarPlanSuscripcion: nombre propio o libre actualiza sin conflicto")
    void actualizarPlanSuscripcion_valido_actualiza() {
        when(planSuscripcionDAO.existsById(PLAN_ID)).thenReturn(true);
        // El mismo plan aparece como "dueño" del nombre: no es conflicto.
        when(planSuscripcionDAO.findByNombreIgnoreCase("Premium")).thenReturn(Optional.of(planConId(PLAN_ID)));
        when(planSuscripcionDAO.update(PLAN_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarPlanSuscripcion(PLAN_ID, dto));
    }

    @Test
    @DisplayName("actualizarPlanSuscripcion: nombre de otro plan lanza BusinessValidationException")
    void actualizarPlanSuscripcion_nombreDeOtro_lanzaBusinessValidation() {
        when(planSuscripcionDAO.existsById(PLAN_ID)).thenReturn(true);
        when(planSuscripcionDAO.findByNombreIgnoreCase("Premium")).thenReturn(Optional.of(planConId(OTRO_ID)));

        assertThrows(BusinessValidationException.class, () -> service.actualizarPlanSuscripcion(PLAN_ID, dto));

        verify(planSuscripcionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarPlanSuscripcion: si el DAO no devuelve el plan actualizado lanza ResourceNotFoundException")
    void actualizarPlanSuscripcion_daoNoActualiza_lanzaNotFound() {
        when(planSuscripcionDAO.existsById(PLAN_ID)).thenReturn(true);
        when(planSuscripcionDAO.findByNombreIgnoreCase("Premium")).thenReturn(Optional.empty());
        when(planSuscripcionDAO.update(PLAN_ID, dto)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarPlanSuscripcion(PLAN_ID, dto));
    }

    // ---------------------------------------------------------------- eliminarPlanSuscripcion

    @Test
    @DisplayName("eliminarPlanSuscripcion: elimina cuando el plan existe")
    void eliminarPlanSuscripcion_existente_elimina() {
        when(planSuscripcionDAO.deleteById(PLAN_ID)).thenReturn(true);

        service.eliminarPlanSuscripcion(PLAN_ID);

        verify(planSuscripcionDAO).deleteById(PLAN_ID);
    }

    @Test
    @DisplayName("eliminarPlanSuscripcion: plan inexistente lanza ResourceNotFoundException")
    void eliminarPlanSuscripcion_inexistente_lanzaNotFound() {
        when(planSuscripcionDAO.deleteById(PLAN_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarPlanSuscripcion(PLAN_ID));
    }
}

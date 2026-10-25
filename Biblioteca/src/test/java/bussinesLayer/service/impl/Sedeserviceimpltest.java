package bussinesLayer.service.impl;

import bussinesLayer.dto.SedeDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.EjemplarDAO;
import persistanceLayer.dao.SedeDAO;

import java.time.LocalTime;
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
@DisplayName("SedeServiceImpl")
class SedeServiceImplTest {

    private static final Long SEDE_ID = 1L;
    private static final Long OTRA_ID = 2L;

    @Mock private SedeDAO sedeDAO;
    @Mock private EjemplarDAO ejemplarDAO;

    @InjectMocks
    private SedeServiceImpl service;

    private SedeDTO dto;

    @BeforeEach
    void setUp() {
        dto = new SedeDTO();
        dto.setNombre("Sede Centro");
        dto.setDireccion("Calle 10 # 5-20");
        dto.setCiudad("Armenia");
        dto.setHoraApertura(LocalTime.of(8, 0));
        dto.setHoraCierre(LocalTime.of(18, 0));
    }

    private SedeDTO sedeConId(Long id) {
        SedeDTO s = new SedeDTO();
        s.setId(id);
        return s;
    }

    // ---------------------------------------------------------------- crearSede

    @Test
    @DisplayName("crearSede: asigna aliada=false y enOperacion=true cuando vienen vacios")
    void crearSede_valida_aplicaDefaults() {
        when(sedeDAO.save(dto)).thenAnswer(inv -> {
            SedeDTO s = inv.getArgument(0);
            s.setId(SEDE_ID);
            return s;
        });

        SedeDTO resultado = service.crearSede(dto);

        assertEquals(SEDE_ID, resultado.getId());
        assertFalse(resultado.getAliada());
        assertTrue(resultado.getEnOperacion());
    }

    @Test
    @DisplayName("crearSede: conserva aliada y enOperacion si ya vienen informados")
    void crearSede_conAliadaYEnOperacion_losConserva() {
        dto.setAliada(true);
        dto.setEnOperacion(false);
        when(sedeDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        SedeDTO resultado = service.crearSede(dto);

        assertTrue(resultado.getAliada());
        assertFalse(resultado.getEnOperacion());
    }

    @Test
    @DisplayName("crearSede: con latitud y longitud juntas la crea")
    void crearSede_conLatitudYLongitud_crea() {
        dto.setLatitud(4.53);
        dto.setLongitud(-75.68);
        when(sedeDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        SedeDTO resultado = service.crearSede(dto);

        assertEquals(4.53, resultado.getLatitud());
        assertEquals(-75.68, resultado.getLongitud());
    }

    @Test
    @DisplayName("crearSede: nombre duplicado lanza BusinessValidationException")
    void crearSede_nombreDuplicado_lanzaBusinessValidation() {
        when(sedeDAO.existsByNombreIgnoreCase("Sede Centro")).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearSede(dto));

        verify(sedeDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearSede: horaCierre anterior a horaApertura lanza BusinessValidationException")
    void crearSede_cierreAnteriorAApertura_lanzaBusinessValidation() {
        dto.setHoraApertura(LocalTime.of(18, 0));
        dto.setHoraCierre(LocalTime.of(8, 0));

        assertThrows(BusinessValidationException.class, () -> service.crearSede(dto));

        verify(sedeDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearSede: horaCierre igual a horaApertura lanza BusinessValidationException")
    void crearSede_cierreIgualAApertura_lanzaBusinessValidation() {
        dto.setHoraApertura(LocalTime.of(8, 0));
        dto.setHoraCierre(LocalTime.of(8, 0));

        assertThrows(BusinessValidationException.class, () -> service.crearSede(dto));

        verify(sedeDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearSede: solo latitud lanza BusinessValidationException")
    void crearSede_soloLatitud_lanzaBusinessValidation() {
        dto.setLatitud(4.53);

        assertThrows(BusinessValidationException.class, () -> service.crearSede(dto));

        verify(sedeDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearSede: solo longitud lanza BusinessValidationException")
    void crearSede_soloLongitud_lanzaBusinessValidation() {
        dto.setLongitud(-75.68);

        assertThrows(BusinessValidationException.class, () -> service.crearSede(dto));

        verify(sedeDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerSedePorId: devuelve la sede cuando existe")
    void obtenerSedePorId_existente_laDevuelve() {
        SedeDTO existente = sedeConId(SEDE_ID);
        when(sedeDAO.findById(SEDE_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerSedePorId(SEDE_ID));
    }

    @Test
    @DisplayName("obtenerSedePorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerSedePorId_inexistente_lanzaNotFound() {
        when(sedeDAO.findById(SEDE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerSedePorId(SEDE_ID));
    }

    @Test
    @DisplayName("obtenerTodasLasSedes: delega en el DAO")
    void obtenerTodasLasSedes_devuelveLista() {
        when(sedeDAO.findAll()).thenReturn(List.of(sedeConId(1L), sedeConId(2L)));

        assertEquals(2, service.obtenerTodasLasSedes().size());
    }

    // ---------------------------------------------------------------- actualizarSede

    @Test
    @DisplayName("actualizarSede: sede inexistente lanza ResourceNotFoundException")
    void actualizarSede_inexistente_lanzaNotFound() {
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarSede(SEDE_ID, dto));

        verify(sedeDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarSede: nombre propio o libre actualiza sin conflicto")
    void actualizarSede_valida_actualiza() {
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        // La misma sede aparece como "dueña" del nombre: no es conflicto.
        when(sedeDAO.findByNombreIgnoreCase("Sede Centro")).thenReturn(Optional.of(sedeConId(SEDE_ID)));
        when(sedeDAO.update(SEDE_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarSede(SEDE_ID, dto));
    }

    @Test
    @DisplayName("actualizarSede: nombre de otra sede lanza BusinessValidationException")
    void actualizarSede_nombreDeOtra_lanzaBusinessValidation() {
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(sedeDAO.findByNombreIgnoreCase("Sede Centro")).thenReturn(Optional.of(sedeConId(OTRA_ID)));

        assertThrows(BusinessValidationException.class, () -> service.actualizarSede(SEDE_ID, dto));

        verify(sedeDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarSede: horario invalido lanza BusinessValidationException")
    void actualizarSede_horarioInvalido_lanzaBusinessValidation() {
        dto.setHoraApertura(LocalTime.of(18, 0));
        dto.setHoraCierre(LocalTime.of(8, 0));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(sedeDAO.findByNombreIgnoreCase("Sede Centro")).thenReturn(Optional.empty());

        assertThrows(BusinessValidationException.class, () -> service.actualizarSede(SEDE_ID, dto));

        verify(sedeDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarSede: coordenadas incompletas lanzan BusinessValidationException")
    void actualizarSede_coordenadasIncompletas_lanzaBusinessValidation() {
        dto.setLatitud(4.53);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(sedeDAO.findByNombreIgnoreCase("Sede Centro")).thenReturn(Optional.empty());

        assertThrows(BusinessValidationException.class, () -> service.actualizarSede(SEDE_ID, dto));

        verify(sedeDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarSede: si el DAO no devuelve la sede actualizada lanza ResourceNotFoundException")
    void actualizarSede_daoNoActualiza_lanzaNotFound() {
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(sedeDAO.findByNombreIgnoreCase("Sede Centro")).thenReturn(Optional.empty());
        when(sedeDAO.update(SEDE_ID, dto)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarSede(SEDE_ID, dto));
    }

    // ---------------------------------------------------------------- eliminarSede

    @Test
    @DisplayName("eliminarSede: sin ejemplares asociados elimina la sede")
    void eliminarSede_sinEjemplares_elimina() {
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(ejemplarDAO.existsBySedeId(SEDE_ID)).thenReturn(false);

        service.eliminarSede(SEDE_ID);

        verify(sedeDAO).deleteById(SEDE_ID);
    }

    @Test
    @DisplayName("eliminarSede: sede inexistente lanza ResourceNotFoundException")
    void eliminarSede_inexistente_lanzaNotFound() {
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarSede(SEDE_ID));

        verify(sedeDAO, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminarSede: con ejemplares asociados lanza BusinessValidationException")
    void eliminarSede_conEjemplares_lanzaBusinessValidation() {
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(ejemplarDAO.existsBySedeId(SEDE_ID)).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.eliminarSede(SEDE_ID));

        verify(sedeDAO, never()).deleteById(any());
    }
}
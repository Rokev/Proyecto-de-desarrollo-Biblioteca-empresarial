package bussinesLayer.service.impl;

import bussinesLayer.dto.CotizacionEnvioDTO;
import bussinesLayer.dto.TarifaEnvioDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.TarifaEnvioDAO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TarifaEnvioServiceImpl")
class TarifaEnvioServiceImplTest {

    private static final Long TARIFA_ID = 1L;
    private static final Long OTRA_ID = 2L;
    private static final BigDecimal COSTO = new BigDecimal("15000");

    @Mock private TarifaEnvioDAO tarifaEnvioDAO;

    @InjectMocks
    private TarifaEnvioServiceImpl service;

    private TarifaEnvioDTO dto;

    @BeforeEach
    void setUp() {
        dto = new TarifaEnvioDTO();
        dto.setCiudadA("Bogota");
        dto.setCiudadB("Medellin");
        dto.setTarifa(COSTO);
    }

    private TarifaEnvioDTO tarifaConId(Long id) {
        TarifaEnvioDTO t = new TarifaEnvioDTO();
        t.setId(id);
        t.setCiudadA("Bogota");
        t.setCiudadB("Medellin");
        t.setTarifa(COSTO);
        return t;
    }

    // ---------------------------------------------------------------- crearTarifa

    @Test
    @DisplayName("crearTarifa: normaliza las ciudades y las deja en orden alfabetico")
    void crearTarifa_ciudadesDesordenadas_lasNormalizaYOrdena() {
        dto.setCiudadA("medellín");
        dto.setCiudadB("BOGOTÁ");
        when(tarifaEnvioDAO.findByPar("Bogota", "Medellin")).thenReturn(Optional.empty());
        when(tarifaEnvioDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        TarifaEnvioDTO resultado = service.crearTarifa(dto);

        assertEquals("Bogota", resultado.getCiudadA());
        assertEquals("Medellin", resultado.getCiudadB());
        verify(tarifaEnvioDAO).save(dto);
    }

    @Test
    @DisplayName("crearTarifa: ciudades ya ordenadas se guardan igual")
    void crearTarifa_ciudadesOrdenadas_guarda() {
        when(tarifaEnvioDAO.findByPar("Bogota", "Medellin")).thenReturn(Optional.empty());
        when(tarifaEnvioDAO.save(dto)).thenAnswer(inv -> {
            TarifaEnvioDTO t = inv.getArgument(0);
            t.setId(TARIFA_ID);
            return t;
        });

        assertEquals(TARIFA_ID, service.crearTarifa(dto).getId());
    }

    @Test
    @DisplayName("crearTarifa: ciudades iguales (tras normalizar) lanza BusinessValidationException")
    void crearTarifa_ciudadesIguales_lanzaBusinessValidation() {
        dto.setCiudadA("bogotá");
        dto.setCiudadB("Bogota");

        assertThrows(BusinessValidationException.class, () -> service.crearTarifa(dto));

        verify(tarifaEnvioDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearTarifa: ciudad vacia lanza BusinessValidationException")
    void crearTarifa_ciudadVacia_lanzaBusinessValidation() {
        dto.setCiudadA("  ");

        assertThrows(BusinessValidationException.class, () -> service.crearTarifa(dto));

        verify(tarifaEnvioDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearTarifa: par ya existente lanza BusinessValidationException")
    void crearTarifa_parDuplicado_lanzaBusinessValidation() {
        when(tarifaEnvioDAO.findByPar("Bogota", "Medellin")).thenReturn(Optional.of(tarifaConId(OTRA_ID)));

        assertThrows(BusinessValidationException.class, () -> service.crearTarifa(dto));

        verify(tarifaEnvioDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerTarifaPorId: devuelve la tarifa cuando existe")
    void obtenerTarifaPorId_existente_laDevuelve() {
        TarifaEnvioDTO existente = tarifaConId(TARIFA_ID);
        when(tarifaEnvioDAO.findById(TARIFA_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerTarifaPorId(TARIFA_ID));
    }

    @Test
    @DisplayName("obtenerTarifaPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerTarifaPorId_inexistente_lanzaNotFound() {
        when(tarifaEnvioDAO.findById(TARIFA_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerTarifaPorId(TARIFA_ID));
    }

    @Test
    @DisplayName("obtenerTodasLasTarifas: delega en el DAO")
    void obtenerTodasLasTarifas_devuelveLista() {
        when(tarifaEnvioDAO.findAll()).thenReturn(List.of(tarifaConId(1L), tarifaConId(2L)));

        assertEquals(2, service.obtenerTodasLasTarifas().size());
    }

    // ---------------------------------------------------------------- actualizarTarifa

    @Test
    @DisplayName("actualizarTarifa: tarifa inexistente lanza ResourceNotFoundException")
    void actualizarTarifa_inexistente_lanzaNotFound() {
        when(tarifaEnvioDAO.findById(TARIFA_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarTarifa(TARIFA_ID, dto));

        verify(tarifaEnvioDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarTarifa: par propio o libre actualiza sin conflicto")
    void actualizarTarifa_valida_actualiza() {
        when(tarifaEnvioDAO.findById(TARIFA_ID)).thenReturn(Optional.of(tarifaConId(TARIFA_ID)));
        // La misma tarifa aparece como "dueña" del par: no es conflicto.
        when(tarifaEnvioDAO.findByPar("Bogota", "Medellin")).thenReturn(Optional.of(tarifaConId(TARIFA_ID)));
        when(tarifaEnvioDAO.update(TARIFA_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarTarifa(TARIFA_ID, dto));
    }

    @Test
    @DisplayName("actualizarTarifa: par que ya tiene otra tarifa lanza BusinessValidationException")
    void actualizarTarifa_parDeOtra_lanzaBusinessValidation() {
        when(tarifaEnvioDAO.findById(TARIFA_ID)).thenReturn(Optional.of(tarifaConId(TARIFA_ID)));
        when(tarifaEnvioDAO.findByPar("Bogota", "Medellin")).thenReturn(Optional.of(tarifaConId(OTRA_ID)));

        assertThrows(BusinessValidationException.class, () -> service.actualizarTarifa(TARIFA_ID, dto));

        verify(tarifaEnvioDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarTarifa: ciudades iguales lanza BusinessValidationException")
    void actualizarTarifa_ciudadesIguales_lanzaBusinessValidation() {
        dto.setCiudadB("bogota");
        when(tarifaEnvioDAO.findById(TARIFA_ID)).thenReturn(Optional.of(tarifaConId(TARIFA_ID)));

        assertThrows(BusinessValidationException.class, () -> service.actualizarTarifa(TARIFA_ID, dto));

        verify(tarifaEnvioDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarTarifa: si el DAO no devuelve la tarifa actualizada lanza ResourceNotFoundException")
    void actualizarTarifa_daoNoActualiza_lanzaNotFound() {
        when(tarifaEnvioDAO.findById(TARIFA_ID)).thenReturn(Optional.of(tarifaConId(TARIFA_ID)));
        when(tarifaEnvioDAO.findByPar("Bogota", "Medellin")).thenReturn(Optional.empty());
        when(tarifaEnvioDAO.update(TARIFA_ID, dto)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarTarifa(TARIFA_ID, dto));
    }

    // ---------------------------------------------------------------- eliminarTarifa

    @Test
    @DisplayName("eliminarTarifa: elimina cuando la tarifa existe")
    void eliminarTarifa_existente_elimina() {
        when(tarifaEnvioDAO.findById(TARIFA_ID)).thenReturn(Optional.of(tarifaConId(TARIFA_ID)));

        service.eliminarTarifa(TARIFA_ID);

        verify(tarifaEnvioDAO).deleteById(TARIFA_ID);
    }

    @Test
    @DisplayName("eliminarTarifa: tarifa inexistente lanza ResourceNotFoundException")
    void eliminarTarifa_inexistente_lanzaNotFound() {
        when(tarifaEnvioDAO.findById(TARIFA_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarTarifa(TARIFA_ID));

        verify(tarifaEnvioDAO, never()).deleteById(any());
    }

    // ---------------------------------------------------------------- buscarCosto

    @Test
    @DisplayName("buscarCosto: ciudades en orden alfabetico encuentran la tarifa")
    void buscarCosto_ordenAlfabetico_devuelveCosto() {
        when(tarifaEnvioDAO.findByPar("Bogota", "Medellin")).thenReturn(Optional.of(tarifaConId(TARIFA_ID)));

        assertEquals(Optional.of(COSTO), service.buscarCosto("Bogota", "Medellin"));
    }

    @Test
    @DisplayName("buscarCosto: ciudades en orden inverso y con tildes encuentran la misma tarifa")
    void buscarCosto_ordenInverso_devuelveCosto() {
        when(tarifaEnvioDAO.findByPar("Bogota", "Medellin")).thenReturn(Optional.of(tarifaConId(TARIFA_ID)));

        assertEquals(Optional.of(COSTO), service.buscarCosto("medellín", "bogotá"));
    }

    @Test
    @DisplayName("buscarCosto: sin tarifa cargada devuelve vacio")
    void buscarCosto_sinTarifa_devuelveVacio() {
        when(tarifaEnvioDAO.findByPar("Bogota", "Cali")).thenReturn(Optional.empty());

        assertTrue(service.buscarCosto("Cali", "Bogota").isEmpty());
    }

    @Test
    @DisplayName("buscarCosto: origen y destino iguales devuelve vacio sin consultar el DAO")
    void buscarCosto_mismaCiudad_devuelveVacio() {
        assertTrue(service.buscarCosto("bogotá", "Bogota").isEmpty());

        verify(tarifaEnvioDAO, never()).findByPar(anyString(), anyString());
    }

    // ---------------------------------------------------------------- calcularCosto

    @Test
    @DisplayName("calcularCosto: devuelve el costo cuando hay tarifa")
    void calcularCosto_conTarifa_devuelveCosto() {
        when(tarifaEnvioDAO.findByPar("Bogota", "Medellin")).thenReturn(Optional.of(tarifaConId(TARIFA_ID)));

        assertEquals(COSTO, service.calcularCosto("Medellin", "Bogota"));
    }

    @Test
    @DisplayName("calcularCosto: sin tarifa lanza BusinessValidationException")
    void calcularCosto_sinTarifa_lanzaBusinessValidation() {
        when(tarifaEnvioDAO.findByPar("Bogota", "Cali")).thenReturn(Optional.empty());

        assertThrows(BusinessValidationException.class, () -> service.calcularCosto("Cali", "Bogota"));
    }

    @Test
    @DisplayName("calcularCosto: origen y destino iguales lanza BusinessValidationException")
    void calcularCosto_mismaCiudad_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class, () -> service.calcularCosto("Bogota", "bogotá"));
    }

    // ---------------------------------------------------------------- cotizar

    @Test
    @DisplayName("cotizar: devuelve ciudades normalizadas y el costo del envio")
    void cotizar_conTarifa_devuelveCotizacion() {
        when(tarifaEnvioDAO.findByPar("Bogota", "Medellin")).thenReturn(Optional.of(tarifaConId(TARIFA_ID)));

        CotizacionEnvioDTO cotizacion = service.cotizar("medellín", "bogotá");

        assertEquals("Medellin", cotizacion.getCiudadOrigen());
        assertEquals("Bogota", cotizacion.getCiudadDestino());
        assertEquals(COSTO, cotizacion.getCostoEnvio());
    }

    @Test
    @DisplayName("cotizar: sin tarifa lanza BusinessValidationException")
    void cotizar_sinTarifa_lanzaBusinessValidation() {
        when(tarifaEnvioDAO.findByPar("Bogota", "Cali")).thenReturn(Optional.empty());

        assertThrows(BusinessValidationException.class, () -> service.cotizar("Bogota", "Cali"));
    }
}

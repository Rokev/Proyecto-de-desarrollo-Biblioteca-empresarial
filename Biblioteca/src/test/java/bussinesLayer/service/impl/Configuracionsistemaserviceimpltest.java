package bussinesLayer.service.impl;

import bussinesLayer.dto.PlazoSistemaDTO;
import bussinesLayer.dto.PoliticaMultaDTO;
import bussinesLayer.dto.PoliticaSancionDTO;
import bussinesLayer.dto.SancionDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.util.ClavePlazo;
import bussinesLayer.util.ConceptoMulta;
import bussinesLayer.util.InfraccionSancion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.PlazoSistemaDAO;
import persistanceLayer.dao.PoliticaMultaDAO;
import persistanceLayer.dao.PoliticaSancionDAO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfiguracionSistemaServiceImpl")
class ConfiguracionSistemaServiceImplTest {

    private static final Long USUARIO_ID = 3L;
    private static final Long PRESTAMO_ID = 8L;

    @Mock private PoliticaSancionDAO politicaSancionDAO;
    @Mock private PoliticaMultaDAO politicaMultaDAO;
    @Mock private PlazoSistemaDAO plazoSistemaDAO;

    @InjectMocks
    private ConfiguracionSistemaServiceImpl service;

    // ---------------------------------------------------------------- helpers

    private PoliticaSancionDTO politicaSancion(String infraccion, String tipo, Integer dias, Boolean activa) {
        PoliticaSancionDTO p = new PoliticaSancionDTO();
        p.setInfraccion(infraccion);
        p.setTipoSancion(tipo);
        p.setDuracionDias(dias);
        p.setActiva(activa);
        return p;
    }

    private PoliticaSancionDTO cuerpoSancion(String tipo, Integer dias, Boolean activa) {
        return politicaSancion(null, tipo, dias, activa);
    }

    private PoliticaMultaDTO cuerpoMulta(BigDecimal monto) {
        PoliticaMultaDTO p = new PoliticaMultaDTO();
        p.setMonto(monto);
        return p;
    }

    private PlazoSistemaDTO cuerpoPlazo(Integer valor) {
        PlazoSistemaDTO p = new PlazoSistemaDTO();
        p.setValor(valor);
        return p;
    }

    /** El DAO real devuelve lo guardado; aquí se devuelve el mismo objeto recibido. */
    private void sancionGuardarDevuelveLoRecibido() {
        when(politicaSancionDAO.guardar(any(PoliticaSancionDTO.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void multaGuardarDevuelveLoRecibido() {
        when(politicaMultaDAO.guardar(any(PoliticaMultaDTO.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void plazoGuardarDevuelveLoRecibido() {
        when(plazoSistemaDAO.guardar(any(PlazoSistemaDTO.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ================================================================ POLÍTICAS DE SANCIÓN

    @Test
    @DisplayName("listarPoliticasSancion: sin datos guardados devuelve los valores por defecto de cada infracción")
    void listarPoliticasSancion_sinDatos_devuelveDefaults() {
        when(politicaSancionDAO.findByInfraccion(anyString())).thenReturn(Optional.empty());

        List<PoliticaSancionDTO> resultado = service.listarPoliticasSancion();

        assertEquals(InfraccionSancion.values().length, resultado.size());
        PoliticaSancionDTO mora = resultado.get(0);
        assertEquals("MORA_DEVOLUCION", mora.getInfraccion());
        assertEquals("BLOQUEO_PRESTAMOS", mora.getTipoSancion());
        assertNull(mora.getDuracionDias());
        assertTrue(mora.getActiva());
    }

    @Test
    @DisplayName("obtenerPoliticaSancion: devuelve la política guardada y acepta minúsculas y espacios")
    void obtenerPoliticaSancion_guardada_laDevuelve() {
        PoliticaSancionDTO guardada = politicaSancion("MORA_DEVOLUCION", "SUSPENSION_CUENTA", 15, true);
        when(politicaSancionDAO.findByInfraccion("MORA_DEVOLUCION")).thenReturn(Optional.of(guardada));

        assertSame(guardada, service.obtenerPoliticaSancion("  mora_devolucion "));
    }

    @Test
    @DisplayName("obtenerPoliticaSancion: sin registro guardado devuelve el valor por defecto")
    void obtenerPoliticaSancion_sinRegistro_devuelveDefault() {
        when(politicaSancionDAO.findByInfraccion("LIBRO_PERDIDO")).thenReturn(Optional.empty());

        PoliticaSancionDTO resultado = service.obtenerPoliticaSancion("LIBRO_PERDIDO");

        assertEquals(30, resultado.getDuracionDias());
        assertFalse(resultado.getActiva());
    }

    @Test
    @DisplayName("obtenerPoliticaSancion: infracción desconocida lanza BusinessValidationException")
    void obtenerPoliticaSancion_desconocida_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class, () -> service.obtenerPoliticaSancion("NO_EXISTE"));
    }

    @Test
    @DisplayName("obtenerPoliticaSancion: infracción nula lanza BusinessValidationException")
    void obtenerPoliticaSancion_nula_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class, () -> service.obtenerPoliticaSancion(null));
    }

    @Test
    @DisplayName("actualizarPoliticaSancion: tipo de sanción inválido lanza BusinessValidationException")
    void actualizarPoliticaSancion_tipoInvalido_lanzaBusinessValidation() {
        PoliticaSancionDTO dto = cuerpoSancion("CASTIGO", 7, true);

        assertThrows(BusinessValidationException.class,
                () -> service.actualizarPoliticaSancion("PEDIDO_NO_RECOGIDO", dto));

        verify(politicaSancionDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizarPoliticaSancion: tipo de sanción nulo lanza BusinessValidationException")
    void actualizarPoliticaSancion_tipoNulo_lanzaBusinessValidation() {
        PoliticaSancionDTO dto = cuerpoSancion(null, 7, true);

        assertThrows(BusinessValidationException.class,
                () -> service.actualizarPoliticaSancion("PEDIDO_NO_RECOGIDO", dto));

        verify(politicaSancionDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizarPoliticaSancion: duración nula solo se permite en MORA_DEVOLUCION")
    void actualizarPoliticaSancion_duracionNulaEnOtraInfraccion_lanzaBusinessValidation() {
        PoliticaSancionDTO dto = cuerpoSancion("BLOQUEO_PRESTAMOS", null, true);

        assertThrows(BusinessValidationException.class,
                () -> service.actualizarPoliticaSancion("PEDIDO_NO_RECOGIDO", dto));

        verify(politicaSancionDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizarPoliticaSancion: duración 0 lanza BusinessValidationException")
    void actualizarPoliticaSancion_duracionCero_lanzaBusinessValidation() {
        PoliticaSancionDTO dto = cuerpoSancion("BLOQUEO_PRESTAMOS", 0, true);

        assertThrows(BusinessValidationException.class,
                () -> service.actualizarPoliticaSancion("PEDIDO_NO_RECOGIDO", dto));

        verify(politicaSancionDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizarPoliticaSancion: duración mayor a 365 días lanza BusinessValidationException")
    void actualizarPoliticaSancion_duracionExcedeMaximo_lanzaBusinessValidation() {
        PoliticaSancionDTO dto = cuerpoSancion("BLOQUEO_PRESTAMOS", 366, true);

        assertThrows(BusinessValidationException.class,
                () -> service.actualizarPoliticaSancion("PEDIDO_NO_RECOGIDO", dto));

        verify(politicaSancionDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizarPoliticaSancion: infracción desconocida lanza BusinessValidationException")
    void actualizarPoliticaSancion_infraccionDesconocida_lanzaBusinessValidation() {
        PoliticaSancionDTO dto = cuerpoSancion("BLOQUEO_PRESTAMOS", 7, true);

        assertThrows(BusinessValidationException.class, () -> service.actualizarPoliticaSancion("NO_EXISTE", dto));

        verify(politicaSancionDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizarPoliticaSancion: datos válidos guardan con tipo normalizado y descripción del catálogo")
    void actualizarPoliticaSancion_valida_guardaNormalizada() {
        when(politicaSancionDAO.findByInfraccion("PEDIDO_NO_RECOGIDO")).thenReturn(Optional.empty());
        sancionGuardarDevuelveLoRecibido();
        PoliticaSancionDTO dto = cuerpoSancion("  suspension_cuenta ", 10, true);

        PoliticaSancionDTO resultado = service.actualizarPoliticaSancion("pedido_no_recogido", dto);

        ArgumentCaptor<PoliticaSancionDTO> captor = ArgumentCaptor.forClass(PoliticaSancionDTO.class);
        verify(politicaSancionDAO).guardar(captor.capture());
        PoliticaSancionDTO guardada = captor.getValue();
        assertEquals("PEDIDO_NO_RECOGIDO", guardada.getInfraccion());
        assertEquals("SUSPENSION_CUENTA", guardada.getTipoSancion());
        assertEquals(10, guardada.getDuracionDias());
        assertTrue(guardada.getActiva());
        assertEquals(InfraccionSancion.PEDIDO_NO_RECOGIDO.getDescripcion(), guardada.getDescripcion());
        assertSame(guardada, resultado);
    }

    @Test
    @DisplayName("actualizarPoliticaSancion: MORA_DEVOLUCION admite duración indefinida (nula)")
    void actualizarPoliticaSancion_moraSinDuracion_esValida() {
        when(politicaSancionDAO.findByInfraccion("MORA_DEVOLUCION")).thenReturn(Optional.empty());
        sancionGuardarDevuelveLoRecibido();

        PoliticaSancionDTO resultado = service.actualizarPoliticaSancion("MORA_DEVOLUCION",
                cuerpoSancion("BLOQUEO_PRESTAMOS", null, true));

        assertNull(resultado.getDuracionDias());
    }

    @Test
    @DisplayName("actualizarPoliticaSancion: duraciones límite 1 y 365 son válidas")
    void actualizarPoliticaSancion_duracionesLimite_sonValidas() {
        when(politicaSancionDAO.findByInfraccion("PEDIDO_NO_RECOGIDO")).thenReturn(Optional.empty());
        sancionGuardarDevuelveLoRecibido();

        PoliticaSancionDTO minima = service.actualizarPoliticaSancion("PEDIDO_NO_RECOGIDO",
                cuerpoSancion("ADVERTENCIA", 1, true));
        PoliticaSancionDTO maxima = service.actualizarPoliticaSancion("PEDIDO_NO_RECOGIDO",
                cuerpoSancion("ADVERTENCIA", 365, true));

        assertEquals(1, minima.getDuracionDias());
        assertEquals(365, maxima.getDuracionDias());
    }

    // ================================================================ POLÍTICAS DE MULTA

    @Test
    @DisplayName("listarPoliticasMulta: sin datos guardados devuelve los montos por defecto de cada concepto")
    void listarPoliticasMulta_sinDatos_devuelveDefaults() {
        when(politicaMultaDAO.findByConcepto(anyString())).thenReturn(Optional.empty());

        List<PoliticaMultaDTO> resultado = service.listarPoliticasMulta();

        assertEquals(ConceptoMulta.values().length, resultado.size());
        PoliticaMultaDTO mora = resultado.get(0);
        assertEquals("MORA_POR_DIA", mora.getConcepto());
        assertEquals(0, new BigDecimal("2000.00").compareTo(mora.getMonto()));
    }

    @Test
    @DisplayName("obtenerPoliticaMulta: devuelve la política guardada")
    void obtenerPoliticaMulta_guardada_laDevuelve() {
        PoliticaMultaDTO guardada = new PoliticaMultaDTO();
        guardada.setConcepto("LIBRO_PERDIDO");
        guardada.setMonto(new BigDecimal("75000.00"));
        when(politicaMultaDAO.findByConcepto("LIBRO_PERDIDO")).thenReturn(Optional.of(guardada));

        assertSame(guardada, service.obtenerPoliticaMulta("libro_perdido"));
    }

    @Test
    @DisplayName("obtenerPoliticaMulta: concepto desconocido lanza BusinessValidationException")
    void obtenerPoliticaMulta_desconocido_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class, () -> service.obtenerPoliticaMulta("NO_EXISTE"));
    }

    @Test
    @DisplayName("actualizarPoliticaMulta: monto nulo lanza BusinessValidationException")
    void actualizarPoliticaMulta_montoNulo_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class,
                () -> service.actualizarPoliticaMulta("MORA_POR_DIA", cuerpoMulta(null)));

        verify(politicaMultaDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizarPoliticaMulta: monto cero lanza BusinessValidationException")
    void actualizarPoliticaMulta_montoCero_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class,
                () -> service.actualizarPoliticaMulta("MORA_POR_DIA", cuerpoMulta(BigDecimal.ZERO)));

        verify(politicaMultaDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizarPoliticaMulta: monto negativo lanza BusinessValidationException")
    void actualizarPoliticaMulta_montoNegativo_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class,
                () -> service.actualizarPoliticaMulta("MORA_POR_DIA", cuerpoMulta(new BigDecimal("-1"))));

        verify(politicaMultaDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizarPoliticaMulta: concepto desconocido lanza BusinessValidationException")
    void actualizarPoliticaMulta_conceptoDesconocido_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class,
                () -> service.actualizarPoliticaMulta("NO_EXISTE", cuerpoMulta(new BigDecimal("1000"))));

        verify(politicaMultaDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizarPoliticaMulta: monto válido guarda concepto, monto y descripción del catálogo")
    void actualizarPoliticaMulta_valida_guarda() {
        when(politicaMultaDAO.findByConcepto("MORA_POR_DIA")).thenReturn(Optional.empty());
        multaGuardarDevuelveLoRecibido();

        PoliticaMultaDTO resultado = service.actualizarPoliticaMulta("mora_por_dia",
                cuerpoMulta(new BigDecimal("2500.00")));

        ArgumentCaptor<PoliticaMultaDTO> captor = ArgumentCaptor.forClass(PoliticaMultaDTO.class);
        verify(politicaMultaDAO).guardar(captor.capture());
        assertEquals("MORA_POR_DIA", captor.getValue().getConcepto());
        assertEquals(new BigDecimal("2500.00"), captor.getValue().getMonto());
        assertEquals(ConceptoMulta.MORA_POR_DIA.getDescripcion(), captor.getValue().getDescripcion());
        assertSame(captor.getValue(), resultado);
    }

    // ================================================================ PLAZOS

    @Test
    @DisplayName("listarPlazos: sin datos guardados devuelve los valores por defecto de cada plazo")
    void listarPlazos_sinDatos_devuelveDefaults() {
        when(plazoSistemaDAO.findByClave(anyString())).thenReturn(Optional.empty());

        List<PlazoSistemaDTO> resultado = service.listarPlazos();

        assertEquals(ClavePlazo.values().length, resultado.size());
        PlazoSistemaDTO prestamo = resultado.get(0);
        assertEquals("DIAS_PRESTAMO", prestamo.getClave());
        assertEquals(14, prestamo.getValor());
        assertEquals("DIAS", prestamo.getUnidad());
    }

    @Test
    @DisplayName("obtenerPlazo: devuelve el plazo guardado")
    void obtenerPlazo_guardado_loDevuelve() {
        PlazoSistemaDTO guardado = new PlazoSistemaDTO();
        guardado.setClave("HORAS_RETIRO_LIBRO");
        guardado.setValor(24);
        when(plazoSistemaDAO.findByClave("HORAS_RETIRO_LIBRO")).thenReturn(Optional.of(guardado));

        assertSame(guardado, service.obtenerPlazo("horas_retiro_libro"));
    }

    @Test
    @DisplayName("obtenerPlazo: clave desconocida lanza BusinessValidationException")
    void obtenerPlazo_desconocida_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class, () -> service.obtenerPlazo("NO_EXISTE"));
    }

    @Test
    @DisplayName("actualizarPlazo: valor nulo lanza BusinessValidationException")
    void actualizarPlazo_valorNulo_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class,
                () -> service.actualizarPlazo("DIAS_PRESTAMO", cuerpoPlazo(null)));

        verify(plazoSistemaDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizarPlazo: valor menor al mínimo lanza BusinessValidationException")
    void actualizarPlazo_valorBajoElMinimo_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class,
                () -> service.actualizarPlazo("DIAS_PRESTAMO", cuerpoPlazo(ClavePlazo.DIAS_PRESTAMO.getMinimo() - 1)));

        verify(plazoSistemaDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizarPlazo: valor mayor al máximo lanza BusinessValidationException")
    void actualizarPlazo_valorSobreElMaximo_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class,
                () -> service.actualizarPlazo("DIAS_PRESTAMO", cuerpoPlazo(ClavePlazo.DIAS_PRESTAMO.getMaximo() + 1)));

        verify(plazoSistemaDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizarPlazo: clave desconocida lanza BusinessValidationException")
    void actualizarPlazo_claveDesconocida_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class,
                () -> service.actualizarPlazo("NO_EXISTE", cuerpoPlazo(10)));

        verify(plazoSistemaDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("actualizarPlazo: valor válido guarda clave, valor, unidad y descripción del catálogo")
    void actualizarPlazo_valido_guarda() {
        when(plazoSistemaDAO.findByClave("DIAS_PRESTAMO")).thenReturn(Optional.empty());
        plazoGuardarDevuelveLoRecibido();

        PlazoSistemaDTO resultado = service.actualizarPlazo("dias_prestamo", cuerpoPlazo(21));

        ArgumentCaptor<PlazoSistemaDTO> captor = ArgumentCaptor.forClass(PlazoSistemaDTO.class);
        verify(plazoSistemaDAO).guardar(captor.capture());
        assertEquals("DIAS_PRESTAMO", captor.getValue().getClave());
        assertEquals(21, captor.getValue().getValor());
        assertEquals("DIAS", captor.getValue().getUnidad());
        assertEquals(ClavePlazo.DIAS_PRESTAMO.getDescripcion(), captor.getValue().getDescripcion());
        assertSame(captor.getValue(), resultado);
    }

    @Test
    @DisplayName("actualizarPlazo: valores límite (mínimo y máximo) son válidos")
    void actualizarPlazo_valoresLimite_sonValidos() {
        when(plazoSistemaDAO.findByClave("HORAS_RETIRO_LIBRO")).thenReturn(Optional.empty());
        plazoGuardarDevuelveLoRecibido();

        PlazoSistemaDTO minimo = service.actualizarPlazo("HORAS_RETIRO_LIBRO",
                cuerpoPlazo(ClavePlazo.HORAS_RETIRO_LIBRO.getMinimo()));
        PlazoSistemaDTO maximo = service.actualizarPlazo("HORAS_RETIRO_LIBRO",
                cuerpoPlazo(ClavePlazo.HORAS_RETIRO_LIBRO.getMaximo()));

        assertEquals(1, minimo.getValor());
        assertEquals(720, maximo.getValor());
    }

    // ================================================================ CONSULTAS PARA OTROS SERVICIOS

    @Test
    @DisplayName("montoMulta: devuelve el monto guardado cuando existe la política")
    void montoMulta_guardado_devuelveElGuardado() {
        PoliticaMultaDTO guardada = new PoliticaMultaDTO();
        guardada.setMonto(new BigDecimal("3000.00"));
        when(politicaMultaDAO.findByConcepto("MORA_POR_DIA")).thenReturn(Optional.of(guardada));

        assertEquals(new BigDecimal("3000.00"), service.montoMulta(ConceptoMulta.MORA_POR_DIA));
    }

    @Test
    @DisplayName("montoMulta: sin política guardada devuelve el monto por defecto")
    void montoMulta_sinRegistro_devuelveDefault() {
        when(politicaMultaDAO.findByConcepto("LIBRO_DANADO")).thenReturn(Optional.empty());

        BigDecimal monto = service.montoMulta(ConceptoMulta.LIBRO_DANADO);

        assertEquals(0, new BigDecimal("30000.00").compareTo(monto));
    }

    @Test
    @DisplayName("plazo: devuelve el valor guardado cuando existe")
    void plazo_guardado_devuelveElGuardado() {
        PlazoSistemaDTO guardado = new PlazoSistemaDTO();
        guardado.setValor(30);
        when(plazoSistemaDAO.findByClave("DIAS_PRESTAMO")).thenReturn(Optional.of(guardado));

        assertEquals(30, service.plazo(ClavePlazo.DIAS_PRESTAMO));
    }

    @Test
    @DisplayName("plazo: sin registro guardado devuelve el valor por defecto")
    void plazo_sinRegistro_devuelveDefault() {
        when(plazoSistemaDAO.findByClave("DIAS_RECOGIDA_SEDE_DEVOLUCION")).thenReturn(Optional.empty());

        assertEquals(2, service.plazo(ClavePlazo.DIAS_RECOGIDA_SEDE_DEVOLUCION));
    }

    // ================================================================ construirSancion

    @Test
    @DisplayName("construirSancion: infracción desactivada por defecto devuelve Optional vacío")
    void construirSancion_politicaInactiva_devuelveVacio() {
        // LIBRO_DANADO viene desactivada por defecto: solo genera multa, no sanción.
        when(politicaSancionDAO.findByInfraccion("LIBRO_DANADO")).thenReturn(Optional.empty());

        Optional<SancionDTO> resultado = service.construirSancion(InfraccionSancion.LIBRO_DANADO,
                USUARIO_ID, PRESTAMO_ID, "Libro dañado");

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("construirSancion: política con activa nulo se trata como inactiva")
    void construirSancion_activaNula_devuelveVacio() {
        when(politicaSancionDAO.findByInfraccion("MORA_DEVOLUCION"))
                .thenReturn(Optional.of(politicaSancion("MORA_DEVOLUCION", "BLOQUEO_PRESTAMOS", 7, null)));

        assertTrue(service.construirSancion(InfraccionSancion.MORA_DEVOLUCION, USUARIO_ID, PRESTAMO_ID, "Mora")
                .isEmpty());
    }

    @Test
    @DisplayName("construirSancion: política activa con duración arma la sanción con fecha fin = aplicación + días")
    void construirSancion_activaConDuracion_armaLaSancion() {
        when(politicaSancionDAO.findByInfraccion("PEDIDO_NO_RECOGIDO")).thenReturn(Optional.empty());

        Optional<SancionDTO> resultado = service.construirSancion(InfraccionSancion.PEDIDO_NO_RECOGIDO,
                USUARIO_ID, PRESTAMO_ID, "No recogió el pedido");

        assertTrue(resultado.isPresent());
        SancionDTO sancion = resultado.get();
        assertEquals("BLOQUEO_PRESTAMOS", sancion.getTipo());
        assertEquals("No recogió el pedido", sancion.getMotivo());
        assertEquals("ACTIVA", sancion.getEstado());
        assertEquals(USUARIO_ID, sancion.getUsuarioId());
        assertEquals(PRESTAMO_ID, sancion.getPrestamoId());
        assertEquals(sancion.getFechaAplicacion().plusDays(7), sancion.getFechaFin());
    }

    @Test
    @DisplayName("construirSancion: duración indefinida (mora por defecto) deja fecha fin en nulo")
    void construirSancion_sinDuracion_fechaFinNula() {
        when(politicaSancionDAO.findByInfraccion("MORA_DEVOLUCION")).thenReturn(Optional.empty());

        SancionDTO sancion = service.construirSancion(InfraccionSancion.MORA_DEVOLUCION, USUARIO_ID, PRESTAMO_ID,
                "Mora").orElseThrow();

        assertNull(sancion.getFechaFin());
        assertEquals("BLOQUEO_PRESTAMOS", sancion.getTipo());
    }

    @Test
    @DisplayName("construirSancion: usa la política guardada y no la de por defecto")
    void construirSancion_politicaGuardada_laUsa() {
        when(politicaSancionDAO.findByInfraccion("MORA_DEVOLUCION"))
                .thenReturn(Optional.of(politicaSancion("MORA_DEVOLUCION", "SUSPENSION_CUENTA", 3, true)));

        SancionDTO sancion = service.construirSancion(InfraccionSancion.MORA_DEVOLUCION, USUARIO_ID, null,
                "Mora").orElseThrow();

        assertEquals("SUSPENSION_CUENTA", sancion.getTipo());
        assertEquals(sancion.getFechaAplicacion().plusDays(3), sancion.getFechaFin());
        assertNull(sancion.getPrestamoId());
    }

    // ================================================================ cargarValoresPorDefecto

    @Test
    @DisplayName("cargarValoresPorDefecto: con la base vacía guarda todas las políticas y plazos")
    void cargarValoresPorDefecto_baseVacia_guardaTodo() {
        when(politicaSancionDAO.findByInfraccion(anyString())).thenReturn(Optional.empty());
        when(politicaMultaDAO.findByConcepto(anyString())).thenReturn(Optional.empty());
        when(plazoSistemaDAO.findByClave(anyString())).thenReturn(Optional.empty());

        service.cargarValoresPorDefecto();

        verify(politicaSancionDAO, times(InfraccionSancion.values().length)).guardar(any());
        verify(politicaMultaDAO, times(ConceptoMulta.values().length)).guardar(any());
        verify(plazoSistemaDAO, times(ClavePlazo.values().length)).guardar(any());
    }

    @Test
    @DisplayName("cargarValoresPorDefecto: si ya existen no sobrescribe nada")
    void cargarValoresPorDefecto_yaExisten_noGuardaNada() {
        when(politicaSancionDAO.findByInfraccion(anyString())).thenReturn(Optional.of(new PoliticaSancionDTO()));
        when(politicaMultaDAO.findByConcepto(anyString())).thenReturn(Optional.of(new PoliticaMultaDTO()));
        when(plazoSistemaDAO.findByClave(anyString())).thenReturn(Optional.of(new PlazoSistemaDTO()));

        service.cargarValoresPorDefecto();

        verify(politicaSancionDAO, never()).guardar(any());
        verify(politicaMultaDAO, never()).guardar(any());
        verify(plazoSistemaDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("cargarValoresPorDefecto: solo guarda las que faltan")
    void cargarValoresPorDefecto_parcial_guardaSoloLasFaltantes() {
        when(politicaSancionDAO.findByInfraccion(anyString())).thenAnswer(inv ->
                "MORA_DEVOLUCION".equals(inv.getArgument(0))
                        ? Optional.of(new PoliticaSancionDTO())
                        : Optional.empty());
        when(politicaMultaDAO.findByConcepto(anyString())).thenReturn(Optional.of(new PoliticaMultaDTO()));
        when(plazoSistemaDAO.findByClave(anyString())).thenReturn(Optional.of(new PlazoSistemaDTO()));

        service.cargarValoresPorDefecto();

        // MORA_DEVOLUCION ya existía: se guardan todas las demás infracciones.
        verify(politicaSancionDAO, times(InfraccionSancion.values().length - 1)).guardar(any());
        verify(politicaMultaDAO, never()).guardar(any());
        verify(plazoSistemaDAO, never()).guardar(any());
    }
}

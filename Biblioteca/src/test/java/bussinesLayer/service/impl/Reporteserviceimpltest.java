package bussinesLayer.service.impl;

import bussinesLayer.dto.ReporteArchivoDTO;
import bussinesLayer.dto.ReporteDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.AuditoriaService;
import bussinesLayer.util.AccionAuditoria;
import bussinesLayer.util.ModuloAuditoria;
import bussinesLayer.util.ReporteCsvExportador;
import bussinesLayer.util.ReportePdfExportador;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.ReporteDAO;
import persistanceLayer.dao.SedeDAO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReporteServiceImpl")
class ReporteServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long SEDE_ID = 3L;

    @Mock private ReporteDAO reporteDAO;
    @Mock private SedeDAO sedeDAO;
    @Mock private AuditoriaService auditoriaService;
    @Mock private ReporteCsvExportador csvExportador;
    @Mock private ReportePdfExportador pdfExportador;

    @InjectMocks
    private ReporteServiceImpl service;

    private static List<Object[]> filas(Object[]... filas) {
        return new ArrayList<>(Arrays.asList(filas));
    }

    // ---------------------------------------------------------------- validaciones

    @Test
    @DisplayName("generarReporte: tipo vacio lanza BusinessValidationException")
    void generar_tipoVacio_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class,
                () -> service.generarReporte(" ", null, null, null, ADMIN_ID));

        verifyNoInteractions(reporteDAO, auditoriaService);
    }

    @Test
    @DisplayName("generarReporte: tipo desconocido lanza BusinessValidationException")
    void generar_tipoInvalido_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class,
                () -> service.generarReporte("inexistente", null, null, null, ADMIN_ID));

        verifyNoInteractions(reporteDAO, auditoriaService);
    }

    @Test
    @DisplayName("generarReporte: fecha hasta anterior a desde lanza BusinessValidationException")
    void generar_rangoInvertido_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class, () -> service.generarReporte("PRESTAMOS",
                LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1), null, ADMIN_ID));

        verifyNoInteractions(reporteDAO, auditoriaService);
    }

    @Test
    @DisplayName("generarReporte: sede inexistente lanza ResourceNotFoundException")
    void generar_sedeInexistente_lanzaNotFound() {
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> service.generarReporte("PRESTAMOS", null, null, SEDE_ID, ADMIN_ID));

        verifyNoInteractions(reporteDAO, auditoriaService);
    }

    // ---------------------------------------------------------------- USO

    @Test
    @DisplayName("generarReporte USO: arma los indicadores, el resumen y registra la auditoria")
    void generar_uso_armaIndicadoresYResumen() {
        when(reporteDAO.contarUsuariosRegistrados(null, null)).thenReturn(5L);
        when(reporteDAO.contarSesionesIniciadas(null, null)).thenReturn(7L);
        when(reporteDAO.contarPrestamos(null, null)).thenReturn(3L);
        when(reporteDAO.contarReservasLibro(null, null)).thenReturn(2L);
        when(reporteDAO.contarReservasSala(null, null)).thenReturn(1L);
        when(reporteDAO.contarInscripcionesEvento(null, null)).thenReturn(4L);
        when(reporteDAO.contarSuscripciones(null, null)).thenReturn(6L);
        when(reporteDAO.contarPedidosInterciudad(null, null)).thenReturn(5L);
        when(reporteDAO.sumarPagosAprobados(null, null)).thenReturn(new BigDecimal("150000.50"));

        ReporteDTO reporte = service.generarReporte("uso", null, null, null, ADMIN_ID);

        assertEquals("USO", reporte.getTipo());
        assertEquals("Reporte de uso del sistema", reporte.getTitulo());
        assertNotNull(reporte.getGeneradoEn());
        assertEquals(List.of("Indicador", "Valor"), reporte.getColumnas());
        assertEquals(9, reporte.getTotalRegistros());
        assertEquals(List.of("Usuarios registrados", "5"), reporte.getFilas().get(0));
        assertEquals(List.of("Pagos aprobados (COP)", "150000.50"), reporte.getFilas().get(8));
        assertEquals("15", reporte.getResumen().get("Actividad total (operaciones)"));
        assertEquals("150000.50", reporte.getResumen().get("Pagos aprobados (COP)"));

        verify(auditoriaService).registrar(eq(ADMIN_ID), eq(AccionAuditoria.GENERAR_REPORTE),
                eq(ModuloAuditoria.REPORTES), isNull(), anyString());
    }

    @Test
    @DisplayName("generarReporte USO: ignora la sede y no la valida")
    void generar_uso_ignoraSede() {
        when(reporteDAO.sumarPagosAprobados(null, null)).thenReturn(BigDecimal.ZERO);

        ReporteDTO reporte = service.generarReporte("USO", null, null, SEDE_ID, ADMIN_ID);

        assertNull(reporte.getSedeId());
        assertEquals("0", reporte.getResumen().get("Actividad total (operaciones)"));
        verify(sedeDAO, never()).existsById(any());
    }

    // ---------------------------------------------------------------- PRESTAMOS

    @Test
    @DisplayName("generarReporte PRESTAMOS: formatea las filas y cuenta por estado y modalidad")
    void generar_prestamos_formateaFilasYResumen() {
        LocalDateTime solicitud = LocalDateTime.of(2026, 10, 1, 9, 30);
        when(reporteDAO.prestamos(null, null, null)).thenReturn(filas(
                new Object[]{1L, "Ana", "Dune", "Sede Centro", "PRESENCIAL", "ACTIVO", solicitud, solicitud.plusDays(7), null},
                new Object[]{2L, "Luis", "Emma", "Sede Norte", "PRESENCIAL", "DEVUELTO", solicitud, solicitud.plusDays(7), solicitud.plusDays(3)}));

        ReporteDTO reporte = service.generarReporte("prestamos", null, null, null, ADMIN_ID);

        assertEquals("PRESTAMOS", reporte.getTipo());
        assertEquals(9, reporte.getColumnas().size());
        assertEquals(2, reporte.getTotalRegistros());
        assertEquals(List.of("1", "Ana", "Dune", "Sede Centro", "PRESENCIAL", "ACTIVO",
                "2026-10-01 09:30", "2026-10-08 09:30", "-"), reporte.getFilas().get(0));
        assertEquals("2", reporte.getResumen().get("Total de préstamos"));
        assertEquals("1", reporte.getResumen().get("Estado ACTIVO"));
        assertEquals("1", reporte.getResumen().get("Estado DEVUELTO"));
        assertEquals("2", reporte.getResumen().get("Modalidad PRESENCIAL"));
    }

    @Test
    @DisplayName("generarReporte PRESTAMOS: aplica el rango de fechas (hasta inclusivo) y la sede")
    void generar_prestamos_conFiltros_convierteFechasYSede() {
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(reporteDAO.prestamos(LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 11, 1, 0, 0), SEDE_ID))
                .thenReturn(new ArrayList<>());

        ReporteDTO reporte = service.generarReporte("PRESTAMOS",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), SEDE_ID, ADMIN_ID);

        assertEquals(SEDE_ID, reporte.getSedeId());
        assertEquals(LocalDate.of(2026, 10, 1), reporte.getDesde());
        assertEquals(LocalDate.of(2026, 10, 31), reporte.getHasta());
        assertEquals(0, reporte.getTotalRegistros());

        ArgumentCaptor<String> descripcion = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(ADMIN_ID), eq(AccionAuditoria.GENERAR_REPORTE),
                eq(ModuloAuditoria.REPORTES), isNull(), descripcion.capture());
        assertEquals("Reporte generado: PRESTAMOS (desde=2026-10-01, hasta=2026-10-31, sede=3, registros=0)",
                descripcion.getValue());
    }

    // ---------------------------------------------------------------- RESERVAS

    @Test
    @DisplayName("generarReporte RESERVAS: une libros y salas ordenadas por fecha de reserva descendente")
    void generar_reservas_unificaYOrdena() {
        LocalDateTime fechaLibro = LocalDateTime.of(2026, 10, 1, 10, 0);
        LocalDateTime fechaSala = LocalDateTime.of(2026, 10, 2, 8, 0);
        when(reporteDAO.reservasLibro(null, null, null)).thenReturn(filas(
                new Object[]{10L, "Ana", "Dune", "Sede Centro", "EN_ESPERA", fechaLibro, fechaLibro.plusDays(2)}));
        when(reporteDAO.reservasSala(null, null, null)).thenReturn(filas(
                new Object[]{20L, "Luis", "Sala A", "Sede Norte", "CONFIRMADA", fechaSala,
                        fechaSala.plusDays(1), fechaSala.plusDays(1).plusHours(2)}));

        ReporteDTO reporte = service.generarReporte("RESERVAS", null, null, null, ADMIN_ID);

        assertEquals(2, reporte.getTotalRegistros());
        assertEquals("Sala", reporte.getFilas().get(0).get(1));
        assertEquals("Libro", reporte.getFilas().get(1).get(1));
        assertEquals("-", reporte.getFilas().get(1).get(7));
        assertEquals("2", reporte.getResumen().get("Total de reservas"));
        assertEquals("1", reporte.getResumen().get("Reservas de libros"));
        assertEquals("1", reporte.getResumen().get("Reservas de salas"));
        assertEquals("1", reporte.getResumen().get("Estado EN_ESPERA"));
        assertEquals("1", reporte.getResumen().get("Estado CONFIRMADA"));
    }

    // ---------------------------------------------------------------- EVENTOS

    @Test
    @DisplayName("generarReporte EVENTOS: calcula la ocupacion por evento y global")
    void generar_eventos_calculaOcupacion() {
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 5, 18, 0);
        when(reporteDAO.eventos(null, null, null)).thenReturn(filas(
                new Object[]{1L, "Club de lectura", "Sede Centro", inicio, inicio.plusHours(2), "PROGRAMADO", 10, 5L},
                new Object[]{2L, "Taller", "Sede Norte", inicio, inicio.plusHours(2), "PROGRAMADO", 20, 5L},
                new Object[]{3L, "Sin cupo", "Sede Norte", inicio, inicio.plusHours(2), "CANCELADO", 0, 0L}));

        ReporteDTO reporte = service.generarReporte("eventos", null, null, null, ADMIN_ID);

        assertEquals(3, reporte.getTotalRegistros());
        assertEquals(9, reporte.getFilas().get(0).size());
        assertEquals("50%", reporte.getFilas().get(0).get(8));
        assertEquals("25%", reporte.getFilas().get(1).get(8));
        assertEquals("0%", reporte.getFilas().get(2).get(8));
        assertEquals("3", reporte.getResumen().get("Total de eventos"));
        assertEquals("30", reporte.getResumen().get("Cupos ofrecidos"));
        assertEquals("10", reporte.getResumen().get("Cupos ocupados"));
        assertEquals("33%", reporte.getResumen().get("Ocupación global"));
        assertEquals("2", reporte.getResumen().get("Estado PROGRAMADO"));
        assertEquals("1", reporte.getResumen().get("Estado CANCELADO"));
    }

    @Test
    @DisplayName("generarReporte EVENTOS: sin eventos la ocupacion global es 0%")
    void generar_eventos_sinDatos_ocupacionCero() {
        ReporteDTO reporte = service.generarReporte("EVENTOS", null, null, null, ADMIN_ID);

        assertEquals(0, reporte.getTotalRegistros());
        assertEquals("0%", reporte.getResumen().get("Ocupación global"));
    }

    // ---------------------------------------------------------------- SUSCRIPCIONES

    @Test
    @DisplayName("generarReporte SUSCRIPCIONES: suma solo las activas, cuenta por estado y plan, e ignora la sede")
    void generar_suscripciones_sumaActivas() {
        LocalDateTime inicio = LocalDateTime.of(2026, 9, 1, 0, 0);
        when(reporteDAO.suscripciones(null, null)).thenReturn(filas(
                new Object[]{1L, "Ana", "Premium", "ACTIVA", inicio, inicio.plusMonths(1), new BigDecimal("50000"), true},
                new Object[]{2L, "Luis", "Basico", "ACTIVA", inicio, inicio.plusMonths(1), new BigDecimal("30000.50"), false},
                new Object[]{3L, "Eva", "Premium", "CANCELADA", inicio, inicio.plusMonths(1), new BigDecimal("10000"), false}));

        ReporteDTO reporte = service.generarReporte("SUSCRIPCIONES", null, null, SEDE_ID, ADMIN_ID);

        assertNull(reporte.getSedeId());
        assertEquals(3, reporte.getTotalRegistros());
        assertEquals("Sí", reporte.getFilas().get(0).get(7));
        assertEquals("No", reporte.getFilas().get(1).get(7));
        assertEquals("50000", reporte.getFilas().get(0).get(6));
        assertEquals("3", reporte.getResumen().get("Total de suscripciones"));
        assertEquals("80000.50", reporte.getResumen().get("Ingreso mensual de suscripciones activas (COP)"));
        assertEquals("2", reporte.getResumen().get("Estado ACTIVA"));
        assertEquals("1", reporte.getResumen().get("Estado CANCELADA"));
        assertEquals("2", reporte.getResumen().get("Plan Premium"));
        assertEquals("1", reporte.getResumen().get("Plan Basico"));
        verify(sedeDAO, never()).existsById(any());
    }

    // ---------------------------------------------------------------- PEDIDOS_INTERCIUDAD

    @Test
    @DisplayName("generarReporte PEDIDOS_INTERCIUDAD: acumula el costo de envio y cuenta por estado")
    void generar_pedidos_acumulaCostoEnvio() {
        LocalDateTime solicitud = LocalDateTime.of(2026, 10, 1, 9, 0);
        when(reporteDAO.pedidosInterciudad(null, null, null)).thenReturn(filas(
                new Object[]{1L, "Ana", "Dune", "Sede Centro", "Sede Norte", "Medellin", "SOLICITADO",
                        12000.0, solicitud, null, "Pedro"},
                new Object[]{2L, "Luis", "Emma", "Sede Centro", "Sede Norte", "Cali", "ENTREGADO",
                        8000.5, solicitud, solicitud.plusDays(2), null},
                new Object[]{3L, "Eva", "Ulises", "Sede Centro", "Sede Norte", "Cali", "SOLICITADO",
                        null, solicitud, null, null}));

        ReporteDTO reporte = service.generarReporte("pedidos-interciudad", null, null, null, ADMIN_ID);

        assertEquals("PEDIDOS_INTERCIUDAD", reporte.getTipo());
        assertEquals(11, reporte.getColumnas().size());
        assertEquals(3, reporte.getTotalRegistros());
        assertEquals("12000.0", reporte.getFilas().get(0).get(7));
        assertEquals("-", reporte.getFilas().get(2).get(7));
        assertEquals("3", reporte.getResumen().get("Total de pedidos"));
        assertEquals("20000.50", reporte.getResumen().get("Costo de envío acumulado (COP)"));
        assertEquals("2", reporte.getResumen().get("Estado SOLICITADO"));
        assertEquals("1", reporte.getResumen().get("Estado ENTREGADO"));
    }

    // ---------------------------------------------------------------- exportarReporte

    @Test
    @DisplayName("exportarReporte: formato vacio lanza BusinessValidationException")
    void exportar_formatoVacio_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class,
                () -> service.exportarReporte("PRESTAMOS", null, null, null, null, ADMIN_ID));

        verifyNoInteractions(reporteDAO, auditoriaService, csvExportador, pdfExportador);
    }

    @Test
    @DisplayName("exportarReporte: formato desconocido lanza BusinessValidationException")
    void exportar_formatoInvalido_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class,
                () -> service.exportarReporte("PRESTAMOS", "xlsx", null, null, null, ADMIN_ID));

        verifyNoInteractions(reporteDAO, auditoriaService, csvExportador, pdfExportador);
    }

    @Test
    @DisplayName("exportarReporte: tipo desconocido lanza BusinessValidationException")
    void exportar_tipoInvalido_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class,
                () -> service.exportarReporte("inexistente", "CSV", null, null, null, ADMIN_ID));

        verifyNoInteractions(auditoriaService, csvExportador, pdfExportador);
    }

    @Test
    @DisplayName("exportarReporte CSV: usa el exportador CSV, arma el nombre y audita")
    void exportar_csv_usaExportadorCsv() {
        byte[] bytes = "id,usuario".getBytes();
        when(csvExportador.exportar(any(ReporteDTO.class))).thenReturn(bytes);

        ReporteArchivoDTO archivo = service.exportarReporte("prestamos", "csv", null, null, null, ADMIN_ID);

        assertTrue(archivo.getNombreArchivo().startsWith("reporte-prestamos-"));
        assertTrue(archivo.getNombreArchivo().endsWith(".csv"));
        assertEquals("text/csv; charset=UTF-8", archivo.getContentType());
        assertArrayEquals(bytes, archivo.getContenido());
        verifyNoInteractions(pdfExportador);

        ArgumentCaptor<String> descripcion = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(ADMIN_ID), eq(AccionAuditoria.EXPORTAR_REPORTE),
                eq(ModuloAuditoria.REPORTES), isNull(), descripcion.capture());
        assertEquals("Reporte exportado a CSV: PRESTAMOS (desde=-, hasta=-, sede=todas, registros=0)",
                descripcion.getValue());
    }

    @Test
    @DisplayName("exportarReporte PDF: usa el exportador PDF y cambia el guion bajo del tipo por guion")
    void exportar_pdf_usaExportadorPdf() {
        byte[] bytes = "%PDF-1.4".getBytes();
        when(pdfExportador.exportar(any(ReporteDTO.class))).thenReturn(bytes);

        ReporteArchivoDTO archivo = service.exportarReporte("PEDIDOS_INTERCIUDAD", "PDF", null, null, null, ADMIN_ID);

        assertTrue(archivo.getNombreArchivo().startsWith("reporte-pedidos-interciudad-"));
        assertTrue(archivo.getNombreArchivo().endsWith(".pdf"));
        assertEquals("application/pdf", archivo.getContentType());
        assertArrayEquals(bytes, archivo.getContenido());
        verifyNoInteractions(csvExportador);
        verify(auditoriaService).registrar(eq(ADMIN_ID), eq(AccionAuditoria.EXPORTAR_REPORTE),
                eq(ModuloAuditoria.REPORTES), isNull(), anyString());
    }
}

package bussinesLayer.service.impl;

import bussinesLayer.dto.ReporteArchivoDTO;
import bussinesLayer.dto.ReporteDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.AuditoriaService;
import bussinesLayer.service.ReporteService;
import bussinesLayer.util.AccionAuditoria;
import bussinesLayer.util.FormatoReporte;
import bussinesLayer.util.ModuloAuditoria;
import bussinesLayer.util.ReporteCsvExportador;
import bussinesLayer.util.ReportePdfExportador;
import bussinesLayer.util.TipoReporte;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.ReporteDAO;
import persistanceLayer.dao.SedeDAO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReporteServiceImpl implements ReporteService {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FECHA_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final ReporteDAO reporteDAO;
    private final SedeDAO sedeDAO;
    private final AuditoriaService auditoriaService;
    private final ReporteCsvExportador csvExportador;
    private final ReportePdfExportador pdfExportador;

    @Override
    public ReporteDTO generarReporte(String tipo, LocalDate desde, LocalDate hasta, Long sedeId, Long administradorId) {
        ReporteDTO reporte = construir(tipo, desde, hasta, sedeId);
        auditoriaService.registrar(administradorId, AccionAuditoria.GENERAR_REPORTE, ModuloAuditoria.REPORTES, null,
                descripcion("Reporte generado", reporte));
        log.info("Reporte {} generado con {} registros", reporte.getTipo(), reporte.getTotalRegistros());
        return reporte;
    }

    @Override
    public ReporteArchivoDTO exportarReporte(String tipo, String formato, LocalDate desde, LocalDate hasta,
                                             Long sedeId, Long administradorId) {
        FormatoReporte fmt = FormatoReporte.desdeTexto(formato);
        ReporteDTO reporte = construir(tipo, desde, hasta, sedeId);
        byte[] contenido = fmt == FormatoReporte.PDF ? pdfExportador.exportar(reporte) : csvExportador.exportar(reporte);
        String nombre = "reporte-" + reporte.getTipo().toLowerCase().replace('_', '-') + "-"
                + reporte.getGeneradoEn().format(FECHA_ARCHIVO) + "." + fmt.getExtension();

        auditoriaService.registrar(administradorId, AccionAuditoria.EXPORTAR_REPORTE, ModuloAuditoria.REPORTES, null,
                descripcion("Reporte exportado a " + fmt.name(), reporte));
        log.info("Reporte {} exportado a {} ({} bytes)", reporte.getTipo(), fmt, contenido.length);
        return new ReporteArchivoDTO(nombre, fmt.getContentType(), contenido);
    }

    // ---------------------------------------------------------------- construcción

    private ReporteDTO construir(String tipoTexto, LocalDate desde, LocalDate hasta, Long sedeId) {
        TipoReporte tipo = TipoReporte.desdeTexto(tipoTexto);
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new BusinessValidationException("La fecha 'hasta' no puede ser anterior a la fecha 'desde'");
        }
        boolean aplicaSede = tipo != TipoReporte.USO && tipo != TipoReporte.SUSCRIPCIONES;
        if (sedeId != null && aplicaSede && !sedeDAO.existsById(sedeId)) {
            throw new ResourceNotFoundException("Sede no encontrada con ID: " + sedeId);
        }
        Long sedeFiltro = aplicaSede ? sedeId : null;
        LocalDateTime inicio = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime finExclusivo = hasta != null ? hasta.plusDays(1).atStartOfDay() : null;

        ReporteDTO reporte = new ReporteDTO();
        reporte.setTipo(tipo.name());
        reporte.setTitulo(tipo.getTitulo());
        reporte.setGeneradoEn(LocalDateTime.now());
        reporte.setDesde(desde);
        reporte.setHasta(hasta);
        reporte.setSedeId(sedeFiltro);

        switch (tipo) {
            case USO -> armarUso(reporte, inicio, finExclusivo);
            case PRESTAMOS -> armarPrestamos(reporte, inicio, finExclusivo, sedeFiltro);
            case RESERVAS -> armarReservas(reporte, inicio, finExclusivo, sedeFiltro);
            case EVENTOS -> armarEventos(reporte, inicio, finExclusivo, sedeFiltro);
            case SUSCRIPCIONES -> armarSuscripciones(reporte, inicio, finExclusivo);
            case PEDIDOS_INTERCIUDAD -> armarPedidos(reporte, inicio, finExclusivo, sedeFiltro);
        }
        reporte.setTotalRegistros(reporte.getFilas().size());
        return reporte;
    }

    private void armarUso(ReporteDTO r, LocalDateTime desde, LocalDateTime hasta) {
        r.setColumnas(List.of("Indicador", "Valor"));
        Map<String, Long> conteos = new LinkedHashMap<>();
        conteos.put("Usuarios registrados", reporteDAO.contarUsuariosRegistrados(desde, hasta));
        conteos.put("Sesiones iniciadas", reporteDAO.contarSesionesIniciadas(desde, hasta));
        conteos.put("Préstamos solicitados", reporteDAO.contarPrestamos(desde, hasta));
        conteos.put("Reservas de libros", reporteDAO.contarReservasLibro(desde, hasta));
        conteos.put("Reservas de salas", reporteDAO.contarReservasSala(desde, hasta));
        conteos.put("Inscripciones a eventos", reporteDAO.contarInscripcionesEvento(desde, hasta));
        conteos.put("Suscripciones iniciadas", reporteDAO.contarSuscripciones(desde, hasta));
        conteos.put("Pedidos interciudad", reporteDAO.contarPedidosInterciudad(desde, hasta));
        conteos.forEach((k, v) -> r.getFilas().add(List.of(k, String.valueOf(v))));

        BigDecimal ingresos = reporteDAO.sumarPagosAprobados(desde, hasta);
        r.getFilas().add(List.of("Pagos aprobados (COP)", ingresos.toPlainString()));
        r.getResumen().put("Actividad total (operaciones)", String.valueOf(
                conteos.get("Préstamos solicitados") + conteos.get("Reservas de libros")
                        + conteos.get("Reservas de salas") + conteos.get("Inscripciones a eventos")
                        + conteos.get("Pedidos interciudad")));
        r.getResumen().put("Pagos aprobados (COP)", ingresos.toPlainString());
    }

    private void armarPrestamos(ReporteDTO r, LocalDateTime desde, LocalDateTime hasta, Long sedeId) {
        r.setColumnas(List.of("ID", "Usuario", "Libro", "Sede de retiro", "Modalidad", "Estado",
                "Fecha solicitud", "Vencimiento", "Fecha devolución"));
        List<Object[]> datos = reporteDAO.prestamos(desde, hasta, sedeId);
        datos.forEach(d -> r.getFilas().add(aTexto(d)));
        r.getResumen().put("Total de préstamos", String.valueOf(datos.size()));
        conteoPor(datos, 5).forEach((estado, n) -> r.getResumen().put("Estado " + estado, String.valueOf(n)));
        conteoPor(datos, 4).forEach((mod, n) -> r.getResumen().put("Modalidad " + mod, String.valueOf(n)));
    }

    private void armarReservas(ReporteDTO r, LocalDateTime desde, LocalDateTime hasta, Long sedeId) {
        r.setColumnas(List.of("ID", "Tipo", "Usuario", "Recurso", "Sede", "Estado",
                "Fecha de reserva", "Inicio", "Fin / límite de retiro"));
        List<Object[]> libros = reporteDAO.reservasLibro(desde, hasta, sedeId);
        List<Object[]> salas = reporteDAO.reservasSala(desde, hasta, sedeId);

        // Se unifican en una sola tabla: [id, tipo, usuario, recurso, sede, estado, fechaReserva, inicio, fin]
        List<Object[]> todas = new ArrayList<>();
        for (Object[] d : libros) {
            todas.add(new Object[]{d[0], "Libro", d[1], d[2], d[3], d[4], d[5], null, d[6]});
        }
        for (Object[] d : salas) {
            todas.add(new Object[]{d[0], "Sala", d[1], d[2], d[3], d[4], d[5], d[6], d[7]});
        }
        todas.sort(Comparator.comparing((Object[] f) -> (LocalDateTime) f[6], Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())).reversed());

        todas.forEach(d -> r.getFilas().add(aTexto(d)));
        r.getResumen().put("Total de reservas", String.valueOf(todas.size()));
        r.getResumen().put("Reservas de libros", String.valueOf(libros.size()));
        r.getResumen().put("Reservas de salas", String.valueOf(salas.size()));
        conteoPor(todas, 5).forEach((estado, n) -> r.getResumen().put("Estado " + estado, String.valueOf(n)));
    }

    private void armarEventos(ReporteDTO r, LocalDateTime desde, LocalDateTime hasta, Long sedeId) {
        r.setColumnas(List.of("ID", "Título", "Sede", "Inicio", "Fin", "Estado", "Cupo máximo", "Cupos ocupados", "Ocupación"));
        List<Object[]> datos = reporteDAO.eventos(desde, hasta, sedeId);
        long cupos = 0;
        long ocupados = 0;
        for (Object[] d : datos) {
            int cupo = ((Number) d[6]).intValue();
            long usados = ((Number) d[7]).longValue();
            cupos += cupo;
            ocupados += usados;
            List<String> fila = new ArrayList<>(aTexto(d));
            fila.add(porcentaje(usados, cupo));
            r.getFilas().add(fila);
        }
        r.getResumen().put("Total de eventos", String.valueOf(datos.size()));
        r.getResumen().put("Cupos ofrecidos", String.valueOf(cupos));
        r.getResumen().put("Cupos ocupados", String.valueOf(ocupados));
        r.getResumen().put("Ocupación global", porcentaje(ocupados, cupos));
        conteoPor(datos, 5).forEach((estado, n) -> r.getResumen().put("Estado " + estado, String.valueOf(n)));
    }

    private void armarSuscripciones(ReporteDTO r, LocalDateTime desde, LocalDateTime hasta) {
        r.setColumnas(List.of("ID", "Usuario", "Plan", "Estado", "Inicio", "Fin", "Valor mensual (COP)", "Renovación automática"));
        List<Object[]> datos = reporteDAO.suscripciones(desde, hasta);
        BigDecimal valorActivas = BigDecimal.ZERO;
        for (Object[] d : datos) {
            r.getFilas().add(aTexto(d));
            if ("ACTIVA".equalsIgnoreCase(String.valueOf(d[3])) && d[6] instanceof BigDecimal v) {
                valorActivas = valorActivas.add(v);
            }
        }
        r.getResumen().put("Total de suscripciones", String.valueOf(datos.size()));
        r.getResumen().put("Ingreso mensual de suscripciones activas (COP)", valorActivas.toPlainString());
        conteoPor(datos, 3).forEach((estado, n) -> r.getResumen().put("Estado " + estado, String.valueOf(n)));
        conteoPor(datos, 2).forEach((plan, n) -> r.getResumen().put("Plan " + plan, String.valueOf(n)));
    }

    private void armarPedidos(ReporteDTO r, LocalDateTime desde, LocalDateTime hasta, Long sedeId) {
        r.setColumnas(List.of("ID", "Usuario", "Libro", "Sede de origen", "Sede de devolución", "Ciudad de entrega",
                "Estado", "Costo de envío (COP)", "Fecha solicitud", "Fecha entrega", "Repartidor"));
        List<Object[]> datos = reporteDAO.pedidosInterciudad(desde, hasta, sedeId);
        double ingresos = 0;
        for (Object[] d : datos) {
            r.getFilas().add(aTexto(d));
            if (d[7] instanceof Number n) {
                ingresos += n.doubleValue();
            }
        }
        r.getResumen().put("Total de pedidos", String.valueOf(datos.size()));
        r.getResumen().put("Costo de envío acumulado (COP)", BigDecimal.valueOf(ingresos).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        conteoPor(datos, 6).forEach((estado, n) -> r.getResumen().put("Estado " + estado, String.valueOf(n)));
    }

    // ---------------------------------------------------------------- utilidades

    private Map<String, Long> conteoPor(List<Object[]> filas, int indice) {
        Map<String, Long> conteo = new LinkedHashMap<>();
        for (Object[] f : filas) {
            conteo.merge(String.valueOf(f[indice]), 1L, Long::sum);
        }
        return conteo;
    }

    private List<String> aTexto(Object[] fila) {
        List<String> texto = new ArrayList<>(fila.length);
        for (Object valor : fila) {
            texto.add(formatear(valor));
        }
        return texto;
    }

    private String formatear(Object valor) {
        if (valor == null) {
            return "-";
        }
        if (valor instanceof LocalDateTime fh) {
            return fh.format(FECHA_HORA);
        }
        if (valor instanceof BigDecimal bd) {
            return bd.toPlainString();
        }
        if (valor instanceof Boolean b) {
            return b ? "Sí" : "No";
        }
        return String.valueOf(valor);
    }

    private String porcentaje(long parte, long total) {
        if (total <= 0) {
            return "0%";
        }
        return Math.round(parte * 100.0 / total) + "%";
    }

    private String descripcion(String base, ReporteDTO r) {
        return base + ": " + r.getTipo() + " (desde=" + (r.getDesde() != null ? r.getDesde() : "-")
                + ", hasta=" + (r.getHasta() != null ? r.getHasta() : "-")
                + ", sede=" + (r.getSedeId() != null ? r.getSedeId() : "todas")
                + ", registros=" + r.getTotalRegistros() + ")";
    }
}

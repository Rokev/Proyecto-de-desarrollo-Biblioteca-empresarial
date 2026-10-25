package bussinesLayer.service;

import bussinesLayer.dto.ReporteArchivoDTO;
import bussinesLayer.dto.ReporteDTO;

import java.time.LocalDate;

/**
 * Lógica de negocio de los reportes del administrador (RF-78): uso, préstamos, reservas, eventos,
 * suscripciones y pedidos interciudad, exportables en PDF y CSV.
 */
public interface ReporteService {

    /**
     * Genera el reporte en memoria. tipo: USO, PRESTAMOS, RESERVAS, EVENTOS, SUSCRIPCIONES o PEDIDOS_INTERCIUDAD.
     * desde/hasta (inclusivas) y sedeId son opcionales; sedeId no aplica a USO ni SUSCRIPCIONES.
     * administradorId (opcional) queda registrado en el log de auditoría.
     */
    ReporteDTO generarReporte(String tipo, LocalDate desde, LocalDate hasta, Long sedeId, Long administradorId);

    /** Genera el reporte y lo exporta. formato: PDF o CSV. */
    ReporteArchivoDTO exportarReporte(String tipo, String formato, LocalDate desde, LocalDate hasta,
                                    Long sedeId, Long administradorId);
}

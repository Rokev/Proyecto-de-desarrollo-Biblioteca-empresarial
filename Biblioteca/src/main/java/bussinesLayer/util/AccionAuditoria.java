package bussinesLayer.util;

import java.util.List;

/** Tipos de acción que se registran en el log de auditoría ( 79). */
public final class AccionAuditoria {

    public static final String CREAR = "CREAR";
    public static final String MODIFICAR = "MODIFICAR";
    public static final String DESACTIVAR = "DESACTIVAR";
    public static final String REACTIVAR = "REACTIVAR";
    public static final String SUSPENDER = "SUSPENDER";
    public static final String ELIMINAR = "ELIMINAR";
    public static final String CONFIGURAR = "CONFIGURAR";
    public static final String GENERAR_REPORTE = "GENERAR_REPORTE";
    public static final String EXPORTAR_REPORTE = "EXPORTAR_REPORTE";

    public static final List<String> TODAS = List.of(CREAR, MODIFICAR, DESACTIVAR, REACTIVAR, SUSPENDER,
            ELIMINAR, CONFIGURAR, GENERAR_REPORTE, EXPORTAR_REPORTE);

    private AccionAuditoria() {
    }
}

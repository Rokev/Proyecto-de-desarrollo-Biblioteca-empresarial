package bussinesLayer.util;

import java.util.List;

/** Módulos del sistema que aparecen en el log de auditoría ( 79). */
public final class ModuloAuditoria {

    public static final String USUARIOS = "USUARIOS";
    public static final String PERSONAL = "PERSONAL"; // bibliotecarios y repartidores
    public static final String SEDES = "SEDES";
    public static final String PLANES = "PLANES";
    public static final String POLITICAS = "POLITICAS"; // sanciones, multas y plazos
    public static final String TARIFAS_ENVIO = "TARIFAS_ENVIO";
    public static final String REPORTES = "REPORTES";
    public static final String CHATBOT = "CHATBOT";

    public static final List<String> TODOS = List.of(USUARIOS, PERSONAL, SEDES, PLANES, POLITICAS,
            TARIFAS_ENVIO, REPORTES, CHATBOT);

    private ModuloAuditoria() {
    }
}

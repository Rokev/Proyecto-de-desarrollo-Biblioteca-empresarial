package bussinesLayer.util;

import bussinesLayer.exception.BusinessValidationException;

import java.util.Arrays;

/** Tipos de reporte que puede generar el administrador (RF-78). */
public enum TipoReporte {

    USO("Reporte de uso del sistema"),
    PRESTAMOS("Reporte de préstamos"),
    RESERVAS("Reporte de reservas"),
    EVENTOS("Reporte de eventos"),
    SUSCRIPCIONES("Reporte de suscripciones"),
    PEDIDOS_INTERCIUDAD("Reporte de pedidos interciudad");

    private final String titulo;

    TipoReporte(String titulo) {
        this.titulo = titulo;
    }

    public String getTitulo() {
        return titulo;
    }

    /** Acepta mayúsculas/minúsculas y guion en lugar de guion bajo (ej. "pedidos-interciudad"). */
    public static TipoReporte desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new BusinessValidationException("El tipo de reporte es obligatorio. Valores válidos: "
                    + Arrays.toString(values()));
        }
        String normalizado = texto.trim().toUpperCase().replace('-', '_');
        try {
            return valueOf(normalizado);
        } catch (IllegalArgumentException e) {
            throw new BusinessValidationException("Tipo de reporte no válido: " + texto
                    + ". Valores válidos: " + Arrays.toString(values()));
        }
    }
}

package bussinesLayer.util;

import bussinesLayer.exception.BusinessValidationException;

import java.util.Arrays;

/** Formatos de exportación de los reportes (RF-78). */
public enum FormatoReporte {

    PDF("application/pdf", "pdf"),
    CSV("text/csv; charset=UTF-8", "csv");

    private final String contentType;
    private final String extension;

    FormatoReporte(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String getContentType() {
        return contentType;
    }

    public String getExtension() {
        return extension;
    }

    public static FormatoReporte desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new BusinessValidationException("El formato es obligatorio. Valores válidos: "
                    + Arrays.toString(values()));
        }
        try {
            return valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessValidationException("Formato no válido: " + texto
                    + ". Valores válidos: " + Arrays.toString(values()));
        }
    }
}

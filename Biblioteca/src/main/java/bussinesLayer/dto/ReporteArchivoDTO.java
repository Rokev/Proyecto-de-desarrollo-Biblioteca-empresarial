package bussinesLayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Archivo exportado de un reporte (PDF o CSV) listo para descargar. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReporteArchivoDTO {

    private String nombreArchivo;
    private String contentType;
    private byte[] contenido;
}

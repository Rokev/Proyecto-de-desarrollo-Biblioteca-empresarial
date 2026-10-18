package presentationLayer.advice;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Estructura estandar de error devuelta por la API")
public record ApiError(
        @Schema(description = "Momento en que ocurrio el error", example = "2026-09-30T15:45:00")
        LocalDateTime timestamp,
        @Schema(description = "Codigo HTTP", example = "404")
        int status,
        @Schema(description = "Descripcion corta del codigo HTTP", example = "Not Found")
        String error,
        @Schema(description = "Detalle del error", example = "Libro no encontrado con id: 99")
        String message,
        @Schema(description = "Ruta solicitada", example = "/api/v1/libros/99")
        String path) {
}

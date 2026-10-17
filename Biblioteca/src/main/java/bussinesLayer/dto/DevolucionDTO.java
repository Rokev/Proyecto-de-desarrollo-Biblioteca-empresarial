package BussisnesCatLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "devolucion de un prestamo")
public class DevolucionDTO {
    @Schema(description = "id unico de devolucion", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "El campo codigoQr es obligatorio")
    @Size(max = 60, message = "El campo codigoQr no puede superar los 60 caracteres")
    @Schema(description = "Codigo alfanumerico o contenido del QR", example = "DEV-2026-0001", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 60)
    private String codigoQr;

    @NotNull(message = "El campo fechaSolicitud es obligatorio")
    @Schema(description = "Fecha de solicitud", example = "2026-10-17T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime fechaSolicitud;

    @Schema(description = "Fecha de confirmacion", example = "2026-10-18T09:00:00")
    private LocalDateTime fechaConfirmacion;

    @Size(max = 20, message = "El campo estado no puede superar los 20 caracteres")
    @Schema(description = "PENDIENTE, CONFIRMADA, RECHAZADA", example = "PENDIENTE", maxLength = 20)
    private String estado;

    @Size(max = 300, message = "El campo observaciones no puede superar los 300 caracteres")
    @Schema(description = "Observaciones", example = "Libro en buen estado", maxLength = 300)
    private String observaciones;

    @NotNull(message = "El campo prestamoId es obligatorio")
    @Schema(description = "id del prestamo", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long prestamoId;

    @NotNull(message = "El campo sedeDevolucionId es obligatorio")
    @Schema(description = "id de la sede donde se entrega el libro", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long sedeDevolucionId;

    @Schema(description = "id del bibliotecario que registra la devolucion", example = "1")
    private Long bibliotecarioId;

}

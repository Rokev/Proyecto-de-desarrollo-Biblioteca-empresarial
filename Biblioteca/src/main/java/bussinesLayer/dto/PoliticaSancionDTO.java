package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Política de sanción: restricción que se aplica al usuario ante una infracción ( 74)")
public class PoliticaSancionDTO {

    @Schema(description = "id único de la política", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Infracción que dispara la sanción (la define la URL, no el cuerpo): MORA_DEVOLUCION, PEDIDO_NO_RECOGIDO, LIBRO_DANADO, LIBRO_PERDIDO",
            example = "MORA_DEVOLUCION", accessMode = Schema.AccessMode.READ_ONLY)
    private String infraccion;

    @NotBlank(message = "El campo tipoSancion es obligatorio")
    @Size(max = 40, message = "El campo tipoSancion no puede superar los 40 caracteres")
    @Schema(description = "BLOQUEO_PRESTAMOS (bloqueo temporal), SUSPENSION_CUENTA o ADVERTENCIA (no bloquea)",
            example = "BLOQUEO_PRESTAMOS", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tipoSancion;

    @Schema(description = "Días que dura la restricción. Nulo = hasta devolver el libro (solo permitido para MORA_DEVOLUCION)", example = "7")
    private Integer duracionDias;

    @NotNull(message = "El campo activa es obligatorio")
    @Schema(description = "Si es false, la infracción no genera sanción", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean activa;

    @Schema(description = "Descripción de la infracción", example = "Retraso en la devolucion de un libro", accessMode = Schema.AccessMode.READ_ONLY)
    private String descripcion;

    @Schema(description = "Última modificación", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime fechaActualizacion;
}

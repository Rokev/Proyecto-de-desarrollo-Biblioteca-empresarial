package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Plazo configurable del sistema (RF-76)")
public class PlazoSistemaDTO {

    @Schema(description = "id único del plazo", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Clave (la define la URL, no el cuerpo): DIAS_PRESTAMO, DIAS_PRESTAMO_INTERCIUDAD, HORAS_RETIRO_LIBRO, DIAS_RECOGIDA_SEDE_DEVOLUCION",
            example = "DIAS_PRESTAMO", accessMode = Schema.AccessMode.READ_ONLY)
    private String clave;

    @NotNull(message = "El campo valor es obligatorio")
    @Schema(description = "Valor del plazo, en la unidad indicada", example = "14", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer valor;

    @Schema(description = "DIAS u HORAS", example = "DIAS", accessMode = Schema.AccessMode.READ_ONLY)
    private String unidad;

    @Schema(description = "Descripción del plazo", accessMode = Schema.AccessMode.READ_ONLY)
    private String descripcion;

    @Schema(description = "Última modificación", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime fechaActualizacion;
}

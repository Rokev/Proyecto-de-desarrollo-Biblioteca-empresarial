package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Tarifa de envío interciudad entre una pareja de ciudades (igual en ambos sentidos)")
public class TarifaEnvioDTO {

    @Schema(description = "id único de la tarifa", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "El campo ciudadA es obligatorio")
    @Size(max = 80, message = "El campo ciudadA no puede superar los 80 caracteres")
    @Schema(description = "Primera ciudad de la pareja", example = "Armenia", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ciudadA;

    @NotBlank(message = "El campo ciudadB es obligatorio")
    @Size(max = 80, message = "El campo ciudadB no puede superar los 80 caracteres")
    @Schema(description = "Segunda ciudad de la pareja", example = "Pereira", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ciudadB;

    @NotNull(message = "El campo tarifa es obligatorio")
    @DecimalMin(value = "0.00", inclusive = false, message = "La tarifa debe ser mayor que cero")
    @Schema(description = "Costo del envío entre las dos ciudades", example = "12000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal tarifa;

    @Schema(description = "Última vez que se creó o modificó la tarifa", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime fechaActualizacion;
}
package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Política de multa: monto configurado para un concepto económico (RF-75)")
public class PoliticaMultaDTO {

    @Schema(description = "id único de la política", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Concepto (lo define la URL, no el cuerpo): MORA_POR_DIA, LIBRO_DANADO, LIBRO_PERDIDO, PEDIDO_NO_RECOGIDO, RECARGO_PEDIDO_NO_RECIBIDO",
            example = "MORA_POR_DIA", accessMode = Schema.AccessMode.READ_ONLY)
    private String concepto;

    @NotNull(message = "El campo monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
    @Schema(description = "Monto en pesos. Para MORA_POR_DIA es el valor por cada día de retraso", example = "2000.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal monto;

    @Schema(description = "Descripción del concepto", accessMode = Schema.AccessMode.READ_ONLY)
    private String descripcion;

    @Schema(description = "Última modificación", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime fechaActualizacion;
}

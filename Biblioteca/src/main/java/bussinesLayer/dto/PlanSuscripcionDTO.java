package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "plan de suscripcion")
public class PlanSuscripcionDTO {
    @Schema(description = "id unico de plan", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "El campo nombre es obligatorio")
    @Size(max = 60, message = "El campo nombre no puede superar los 60 caracteres")
    @Schema(description = "Nombre del plan", example = "Premium", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 60)
    private String nombre;

    @Size(max = 300, message = "El campo descripcion no puede superar los 300 caracteres")
    @Schema(description = "Descripcion", example = "Plan con todos los beneficios", maxLength = 300)
    private String descripcion;

    @Schema(description = "Beneficios del plan", example = "Descuento en salas, domicilio, eventos")
    private String beneficios;

    @NotNull(message = "El campo precio es obligatorio")
    @DecimalMin(value = "0.00", message = "El campo precio no puede ser negativo")
    @Schema(description = "Precio", example = "30000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal precio;

    @NotNull(message = "El campo duracionMeses es obligatorio")
    @Min(value = 1, message = "El campo duracionMeses debe ser al menos 1")
    @Schema(description = "Vigencia en meses", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer duracionMeses;

    @NotNull(message = "El campo descuentoSalas es obligatorio")
    @DecimalMin(value = "0.0", message = "El campo descuentoSalas no puede ser negativo")
    @DecimalMax(value = "1.0", message = "El campo descuentoSalas no puede superar 1.0 (100%)")
    @Schema(description = "Descuento en salas de estudio como fraccion entre 0 y 1 (0.15 = 15%)", example = "0.15", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal descuentoSalas;

    @Schema(description = "Indica si el plan esta activo", example = "true")
    private Boolean activo;

}

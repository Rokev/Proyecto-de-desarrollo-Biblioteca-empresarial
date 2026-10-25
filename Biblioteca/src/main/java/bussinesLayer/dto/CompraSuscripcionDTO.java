package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "solicitud de compra de una suscripcion (crea la suscripcion y su pago en un solo paso)")
public class CompraSuscripcionDTO {

    @NotNull(message = "El campo usuarioId es obligatorio")
    @Schema(description = "id del usuario que compra", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long usuarioId;

    @NotNull(message = "El campo planId es obligatorio")
    @Schema(description = "id del plan que se compra", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long planId;

    @Schema(description = "Renovacion automatica (por defecto false)", example = "false")
    private Boolean renovacionAutomatica;
}
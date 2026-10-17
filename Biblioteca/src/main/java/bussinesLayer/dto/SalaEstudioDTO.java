package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "sala de estudio de una sede")
public class SalaEstudioDTO {
    @Schema(description = "id unico de sala", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "El campo nombre es obligatorio")
    @Size(max = 80, message = "El campo nombre no puede superar los 80 caracteres")
    @Schema(description = "Nombre de la sala", example = "Sala 1", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 80)
    private String nombre;

    @NotNull(message = "El campo capacidad es obligatorio")
    @Min(value = 1, message = "El campo capacidad debe ser al menos 1")
    @Schema(description = "Capacidad de personas", example = "6", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer capacidad;

    @Size(max = 300, message = "El campo descripcion no puede superar los 300 caracteres")
    @Schema(description = "Descripcion", example = "Sala con tablero y proyector", maxLength = 300)
    private String descripcion;

    @NotNull(message = "El campo tarifaPorHora es obligatorio")
    @DecimalMin(value = "0.00", message = "El campo tarifaPorHora no puede ser negativo")
    @Schema(description = "Tarifa por hora", example = "10000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal tarifaPorHora;

    @Schema(description = "Indica si la sala esta activa", example = "true")
    private Boolean activa;

    @NotNull(message = "El campo sedeId es obligatorio")
    @Schema(description = "id de la sede", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long sedeId;

}

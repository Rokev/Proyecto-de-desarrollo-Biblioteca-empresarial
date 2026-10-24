package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "solicitud de devolucion de un prestamo en cualquier sede")
public class SolicitudDevolucionDTO {

    @NotNull(message = "El campo prestamoId es obligatorio")
    @Schema(description = "id del prestamo que se devuelve", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long prestamoId;

    @NotNull(message = "El campo usuarioId es obligatorio")
    @Schema(description = "id del usuario dueño del prestamo", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long usuarioId;

    @Schema(description = "id de la sede donde se entregara el libro. Si se omite, el sistema elige la mas cercana usando latitud y longitud", example = "2")
    private Long sedeDevolucionId;

    @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
    @Schema(description = "Latitud del usuario (para elegir la sede mas cercana)", example = "6.2442")
    private Double latitud;

    @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
    @Schema(description = "Longitud del usuario (para elegir la sede mas cercana)", example = "-75.5658")
    private Double longitud;
}
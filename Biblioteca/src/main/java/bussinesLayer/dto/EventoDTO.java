package BussisnesCatLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
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
@Schema(description = "evento organizado en una sede")
public class EventoDTO {
    @Schema(description = "id unico de evento", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "El campo titulo es obligatorio")
    @Size(max = 150, message = "El campo titulo no puede superar los 150 caracteres")
    @Schema(description = "Titulo del evento", example = "Club de lectura", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 150)
    private String titulo;

    @Schema(description = "Descripcion", example = "Charla sobre novela latinoamericana")
    private String descripcion;

    @NotNull(message = "El campo fechaInicio es obligatorio")
    @Schema(description = "Fecha de inicio", example = "2026-11-01T18:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime fechaInicio;

    @NotNull(message = "El campo fechaFin es obligatorio")
    @Schema(description = "Fecha de fin", example = "2026-11-01T20:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime fechaFin;

    @NotNull(message = "El campo cupoMaximo es obligatorio")
    @Min(value = 1, message = "El campo cupoMaximo debe ser al menos 1")
    @Schema(description = "Cupo maximo", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer cupoMaximo;

    @NotNull(message = "El campo cuposDisponibles es obligatorio")
    @Min(value = 0, message = "El campo cuposDisponibles no puede ser negativo")
    @Schema(description = "Cupos disponibles", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer cuposDisponibles;

    @Size(max = 20, message = "El campo estado no puede superar los 20 caracteres")
    @Schema(description = "PROGRAMADO, CANCELADO, FINALIZADO", example = "PROGRAMADO", maxLength = 20)
    private String estado;

    @NotNull(message = "El campo sedeId es obligatorio")
    @Schema(description = "id de la sede", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long sedeId;

}

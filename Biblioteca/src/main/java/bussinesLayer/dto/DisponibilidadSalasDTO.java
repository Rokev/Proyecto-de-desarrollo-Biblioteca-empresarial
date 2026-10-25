package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Disponibilidad de las salas de estudio de una sede en un rango de tiempo")
public class DisponibilidadSalasDTO {
    @Schema(description = "ID de la sede")
    private Long sedeId;

    @Schema(description = "true si la sede tiene al menos una sala de estudio activa")
    private boolean tieneSalas;

    @Schema(description = "Numero de salas activas de la sede")
    private int totalSalas;

    @Schema(description = "Numero de salas libres en el rango consultado")
    private int salasDisponibles;

    @Schema(description = "Inicio del rango consultado")
    private LocalDateTime inicio;

    @Schema(description = "Fin del rango consultado")
    private LocalDateTime fin;

    @Schema(description = "Estado de cada sala en el rango consultado")
    private List<SalaDisponibilidad> salas;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SalaDisponibilidad {
        private Long salaId;
        private String nombre;
        private Integer capacidad;
        private BigDecimal tarifaPorHora;
        private boolean disponible;
    }
}

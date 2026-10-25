package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Asistente inscrito a un evento")
public class AsistenteEventoDTO {
    @Schema(description = "ID de la inscripcion")
    private Long inscripcionId;

    @Schema(description = "ID del usuario")
    private Long usuarioId;

    @Schema(description = "Nombre completo")
    private String nombre;

    @Schema(description = "Correo del usuario")
    private String correo;

    @Schema(description = "Cupos reservados")
    private Integer cantidadCupos;

    @Schema(description = "ACTIVA o CANCELADA")
    private String estado;

    @Schema(description = "Fecha de inscripcion")
    private LocalDateTime fechaInscripcion;
}

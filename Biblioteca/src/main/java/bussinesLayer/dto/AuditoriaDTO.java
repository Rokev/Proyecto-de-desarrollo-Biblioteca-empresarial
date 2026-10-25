package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Registro del historial de auditoría del sistema")
public class AuditoriaDTO {

    @Schema(description = "id del registro", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Fecha y hora de la acción", example = "2026-10-25T14:30:00")
    private LocalDateTime fecha;

    @Schema(description = "id del usuario que realizó la acción (nulo si fue el sistema)", example = "1")
    private Long usuarioId;

    @Schema(description = "Nombre del usuario que realizó la acción", example = "admin")
    private String usuarioNombre;

    @Schema(description = "Tipo de acción", example = "MODIFICAR")
    private String accion;

    @Schema(description = "Módulo del sistema donde ocurrió", example = "SEDES")
    private String modulo;

    @Schema(description = "id del registro afectado", example = "3")
    private Long entidadId;

    @Schema(description = "Descripción de lo ocurrido", example = "Sede 'Centro' desactivada")
    private String descripcion;
}

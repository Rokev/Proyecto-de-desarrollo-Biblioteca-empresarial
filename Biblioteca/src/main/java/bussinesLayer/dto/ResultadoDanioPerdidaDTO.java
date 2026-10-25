package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Resultado de registrar un libro devuelto como dañado o perdido (RF-41)")
public class ResultadoDanioPerdidaDTO {

    @Schema(description = "id del préstamo que se cerró", example = "1")
    private Long prestamoId;

    @Schema(description = "id del ejemplar afectado", example = "5")
    private Long ejemplarId;

    @Schema(description = "Nuevo estado del ejemplar: DANADO o PERDIDO (ya no cuenta como disponible)", example = "DANADO")
    private String estadoEjemplar;

    @Schema(description = "Multa generada al usuario con el monto configurado (RF-75) o el indicado por el bibliotecario")
    private MultaDTO multa;
}

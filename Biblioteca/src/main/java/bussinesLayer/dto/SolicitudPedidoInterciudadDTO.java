package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "solicitud de un libro de otra ciudad con entrega a domicilio")
public class SolicitudPedidoInterciudadDTO {

    @NotNull(message = "El campo usuarioId es obligatorio")
    @Schema(description = "id del usuario con suscripcion", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long usuarioId;

    @NotNull(message = "El campo libroId es obligatorio")
    @Schema(description = "id del libro que se pide", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long libroId;

    @NotBlank(message = "El campo ciudadEntrega es obligatorio")
    @Size(max = 80, message = "El campo ciudadEntrega no puede superar los 80 caracteres")
    @Schema(description = "Ciudad donde se entrega el libro (el ejemplar se busca en otras ciudades)", example = "Medellin", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 80)
    private String ciudadEntrega;

    @NotBlank(message = "El campo direccionEntrega es obligatorio")
    @Size(max = 200, message = "El campo direccionEntrega no puede superar los 200 caracteres")
    @Schema(description = "Direccion a la que llega el libro", example = "Calle 10 # 20-30", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
    private String direccionEntrega;
}
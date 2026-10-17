package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "sede de la red de bibliotecas")
public class SedeDTO {
    @Schema(description = "id unico de sede", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "El campo nombre es obligatorio")
    @Size(max = 120, message = "El campo nombre no puede superar los 120 caracteres")
    @Schema(description = "Nombre de la sede", example = "Sede Centro", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 120)
    private String nombre;

    @NotBlank(message = "El campo direccion es obligatorio")
    @Size(max = 200, message = "El campo direccion no puede superar los 200 caracteres")
    @Schema(description = "Direccion", example = "calle 35 n34-57", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
    private String direccion;

    @NotBlank(message = "El campo ciudad es obligatorio")
    @Size(max = 80, message = "El campo ciudad no puede superar los 80 caracteres")
    @Schema(description = "Ciudad", example = "Bogota", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 80)
    private String ciudad;

    @Size(max = 20, message = "El campo telefono no puede superar los 20 caracteres")
    @Schema(description = "Telefono", example = "3001234567", maxLength = 20)
    private String telefono;

    @Size(max = 120, message = "El campo correo no puede superar los 120 caracteres")
    @Email(message = "El correo no tiene un formato valido")
    @Schema(description = "Correo de la sede", example = "centro@biblioteca.com", maxLength = 120)
    private String correo;

    @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
    @Schema(description = "Latitud", example = "4.6097")
    private Double latitud;

    @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
    @Schema(description = "Longitud", example = "-74.0817")
    private Double longitud;

    @Schema(description = "Hora de apertura", example = "08:00:00")
    private LocalTime horaApertura;

    @Schema(description = "Hora de cierre", example = "18:00:00")
    private LocalTime horaCierre;

    @Schema(description = "true si es sede aliada; false si es propia", example = "false")
    private Boolean aliada;

    @Schema(description = "Indica si la sede esta en operacion", example = "true")
    private Boolean enOperacion;

}

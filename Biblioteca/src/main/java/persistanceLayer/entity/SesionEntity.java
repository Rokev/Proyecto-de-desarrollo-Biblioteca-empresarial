package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
/**
 * Sesión activa de un usuario.
 * Se crea al iniciar sesión y se cierra al cerrar sesión o al expirar.
 */
@Entity
@Table(name = "sesiones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SesionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @Column(nullable = false)
    private LocalDateTime fechaInicio;

    @Column(nullable = false)
    private LocalDateTime fechaExpiracion;

    /** Se llena al cerrar sesión. */
    private LocalDateTime fechaCierre;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;
}

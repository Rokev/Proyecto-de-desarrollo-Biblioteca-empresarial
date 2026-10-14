package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Persona que usa el sistema: cliente (con o sin suscripción) o personal de la
 * biblioteca (gestor / bibliotecaria). El tipo lo define {@link Rol}.
 */
@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String apellido;

    @Column(nullable = false, unique = true, length = 20)
    private String documento;

    @Column(nullable = false, unique = true, length = 120)
    private String correo;

    /** Hash de la contraseña (nunca en texto plano). */
    @Column(nullable = false, length = 255)
    private String contrasena;

    @Column(length = 20)
    private String telefono;

    @Column(length = 200)
    private String direccion;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private RolEntity rol;

    /** Sede donde trabaja (solo para gestores / bibliotecarias). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id")
    private SedeEntity sede;

    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PrestamoEntity> prestamos = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    @Builder.Default
    private List<MultaEntity> multas = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    @Builder.Default
    private List<SuscripcionEntity> suscripciones = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MetodoPagoEntity> metodosPago = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PagoEntity> pagos = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    @Builder.Default
    private List<ReservaSalaEntity> reservasSala = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    @Builder.Default
    private List<InscripcionEventoEntity> inscripcionesEvento = new ArrayList<>();

}

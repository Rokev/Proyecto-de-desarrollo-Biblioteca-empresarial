package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Builder;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sede de la red de bibliotecas (propia o aliada).
 */
@Entity
@Table(name = "sedes")
@Data 
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SedeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String nombre;

    @Column(nullable = false, length = 200)
    private String direccion;

    @Column(nullable = false, length = 80)
    private String ciudad;

    @Column(length = 20)
    private String telefono;

    @Column(length = 120)
    private String correo;

    /** Coordenadas para la búsqueda de la sede más cercana por GPS. */
    private Double latitud;

    private Double longitud;

    private LocalTime horaApertura;

    private LocalTime horaCierre;

    /** true si es una sede aliada; false si es propia. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean aliada = false;

    /** Indica si la sede está en operación (no se puede eliminar si lo está). */
    @Column(nullable = false)
    @Builder.Default
    private Boolean enOperacion = true;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "sede", fetch = FetchType.LAZY)
    @Builder.Default
    private List<UsuarioEntity> personal = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "sede", fetch = FetchType.LAZY)
    @Builder.Default
    private List<EjemplarEntity> ejemplares = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "sede", fetch = FetchType.LAZY)
    @Builder.Default
    private List<SalaEstudioEntity> salasEstudio = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "sede", fetch = FetchType.LAZY)
    @Builder.Default
    private List<EventoEntity> eventos = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "sedeDevolucion", fetch = FetchType.LAZY)
    @Builder.Default
    private List<DevolucionEntity> devolucionesRecibidas = new ArrayList<>();
}

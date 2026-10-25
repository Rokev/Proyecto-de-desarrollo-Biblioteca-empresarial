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
 * Evento organizado en una sede (los suscriptores tienen entrada incluida).
 */
@Entity
@Table(name = "eventos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime fechaInicio;

    @Column(nullable = false)
    private LocalDateTime fechaFin;

    @Column(nullable = false)
    private Integer cupoMaximo;

    /** Cupos libres actualizados al inscribir / cancelar. */
    @Column(nullable = false)
    private Integer cuposDisponibles;

    /** PROGRAMADO, CANCELADO, FINALIZADO. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "PROGRAMADO";

        /** true si el evento es solo para suscriptores con suscripción vigente. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean exclusivo = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private SedeEntity sede;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "evento", fetch = FetchType.LAZY)
    @Builder.Default
    private List<InscripcionEventoEntity> inscripciones = new ArrayList<>();
}
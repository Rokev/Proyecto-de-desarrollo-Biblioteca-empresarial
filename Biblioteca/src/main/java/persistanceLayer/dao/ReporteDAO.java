package persistanceLayer.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consultas de solo lectura que alimentan los reportes ( 78).
 * Cada método devuelve filas "planas" (Object[]) ya filtradas por rango de fechas y, cuando aplica, por sede;
 * el servicio se encarga de darles formato. {@code desde} es inclusivo y {@code hasta} exclusivo; ambos opcionales.
 * Los filtros se arman dinámicamente solo si vienen informados (los nombres de entidad y campo son constantes
 * internas, los valores siempre viajan como parámetros).
 */
@Repository
public class ReporteDAO {

    private static final String NOMBRE_COMPLETO_USUARIO = "concat(u.nombre, ' ', u.apellido)";

    @PersistenceContext
    private EntityManager em;

    /** id, usuario, libro, sede de retiro, modalidad, estado, fecha solicitud, vencimiento, fecha devolución. */
    public List<Object[]> prestamos(LocalDateTime desde, LocalDateTime hasta, Long sedeId) {
        Filtro f = new Filtro();
        f.rango("p.fechaSolicitud", desde, hasta);
        f.sede("p.sedeRetiro.id", sedeId);
        return ejecutar("SELECT p.id, " + NOMBRE_COMPLETO_USUARIO + ", l.titulo, s.nombre, p.modalidad, p.estado, "
                + "p.fechaSolicitud, p.fechaVencimiento, p.fechaDevolucion "
                + "FROM PrestamoEntity p JOIN p.usuario u JOIN p.ejemplar e JOIN e.libro l JOIN p.sedeRetiro s"
                + f.where() + " ORDER BY p.fechaSolicitud DESC", f);
    }

    /** id, usuario, libro, sede, estado, fecha reserva, fecha límite de retiro. */
    public List<Object[]> reservasLibro(LocalDateTime desde, LocalDateTime hasta, Long sedeId) {
        Filtro f = new Filtro();
        f.rango("r.fechaReserva", desde, hasta);
        f.sede("r.sede.id", sedeId);
        return ejecutar("SELECT r.id, " + NOMBRE_COMPLETO_USUARIO + ", l.titulo, s.nombre, r.estado, "
                + "r.fechaReserva, r.fechaLimiteRetiro "
                + "FROM ReservaLibroEntity r JOIN r.usuario u JOIN r.libro l JOIN r.sede s"
                + f.where() + " ORDER BY r.fechaReserva DESC", f);
    }

    /** id, usuario, sala, sede, estado, fecha de creación, inicio, fin. */
    public List<Object[]> reservasSala(LocalDateTime desde, LocalDateTime hasta, Long sedeId) {
        Filtro f = new Filtro();
        f.rango("r.fechaCreacion", desde, hasta);
        f.sede("sa.sede.id", sedeId);
        return ejecutar("SELECT r.id, " + NOMBRE_COMPLETO_USUARIO + ", sa.nombre, s.nombre, r.estado, "
                + "r.fechaCreacion, r.fechaInicio, r.fechaFin "
                + "FROM ReservaSalaEntity r JOIN r.usuario u JOIN r.sala sa JOIN sa.sede s"
                + f.where() + " ORDER BY r.fechaCreacion DESC", f);
    }

    /** id, título, sede, inicio, fin, estado, cupo máximo, cupos ocupados (inscripciones activas). */
    public List<Object[]> eventos(LocalDateTime desde, LocalDateTime hasta, Long sedeId) {
        Filtro f = new Filtro();
        f.rango("e.fechaInicio", desde, hasta);
        f.sede("e.sede.id", sedeId);
        return ejecutar("SELECT e.id, e.titulo, s.nombre, e.fechaInicio, e.fechaFin, e.estado, e.cupoMaximo, "
                + "(SELECT COALESCE(SUM(i.cantidadCupos), 0) FROM InscripcionEventoEntity i "
                + "WHERE i.evento = e AND i.estado = 'ACTIVA') "
                + "FROM EventoEntity e JOIN e.sede s"
                + f.where() + " ORDER BY e.fechaInicio DESC", f);
    }

    /** id, usuario, plan, estado, inicio, fin, valor mensual, renovación automática. Filtra por fecha de inicio. */
    public List<Object[]> suscripciones(LocalDateTime desde, LocalDateTime hasta) {
        Filtro f = new Filtro();
        f.rango("s.fechaInicio", desde != null ? desde.toLocalDate() : null, hasta != null ? hasta.toLocalDate() : null);
        return ejecutar("SELECT s.id, " + NOMBRE_COMPLETO_USUARIO + ", pl.nombre, s.estado, s.fechaInicio, s.fechaFin, "
                + "s.valorMensual, s.renovacionAutomatica "
                + "FROM SuscripcionEntity s JOIN s.usuario u JOIN s.plan pl"
                + f.where() + " ORDER BY s.fechaInicio DESC", f);
    }

    /**
     * id, usuario, libro, sede de origen, sede de devolución, ciudad de entrega, estado, costo de envío,
     * fecha solicitud, fecha entrega, repartidor (puede ser nulo). La sede filtra por origen o devolución.
     */
    public List<Object[]> pedidosInterciudad(LocalDateTime desde, LocalDateTime hasta, Long sedeId) {
        Filtro f = new Filtro();
        f.rango("p.fechaSolicitud", desde, hasta);
        if (sedeId != null) {
            f.condicion("(p.sedeOrigen.id = :sede OR p.sedeDevolucion.id = :sede)", "sede", sedeId);
        }
        return ejecutar("SELECT p.id, " + NOMBRE_COMPLETO_USUARIO + ", l.titulo, so.nombre, sd.nombre, p.ciudadEntrega, "
                + "p.estado, p.costoEnvio, p.fechaSolicitud, p.fechaEntrega, concat(rp.nombre, ' ', rp.apellido) "
                + "FROM PedidoInterciudadEntity p JOIN p.usuario u JOIN p.ejemplar e JOIN e.libro l "
                + "JOIN p.sedeOrigen so JOIN p.sedeDevolucion sd LEFT JOIN p.repartidor rp"
                + f.where() + " ORDER BY p.fechaSolicitud DESC", f);
    }

    // ---- Conteos para el reporte de uso ----

    public long contarUsuariosRegistrados(LocalDateTime desde, LocalDateTime hasta) {
        return contar("UsuarioEntity", "fechaRegistro", desde, hasta, false);
    }

    public long contarSesionesIniciadas(LocalDateTime desde, LocalDateTime hasta) {
        return contar("SesionEntity", "fechaInicio", desde, hasta, false);
    }

    public long contarPrestamos(LocalDateTime desde, LocalDateTime hasta) {
        return contar("PrestamoEntity", "fechaSolicitud", desde, hasta, false);
    }

    public long contarReservasLibro(LocalDateTime desde, LocalDateTime hasta) {
        return contar("ReservaLibroEntity", "fechaReserva", desde, hasta, false);
    }

    public long contarReservasSala(LocalDateTime desde, LocalDateTime hasta) {
        return contar("ReservaSalaEntity", "fechaCreacion", desde, hasta, false);
    }

    public long contarInscripcionesEvento(LocalDateTime desde, LocalDateTime hasta) {
        return contar("InscripcionEventoEntity", "fechaInscripcion", desde, hasta, false);
    }

    public long contarSuscripciones(LocalDateTime desde, LocalDateTime hasta) {
        return contar("SuscripcionEntity", "fechaInicio", desde, hasta, true);
    }

    public long contarPedidosInterciudad(LocalDateTime desde, LocalDateTime hasta) {
        return contar("PedidoInterciudadEntity", "fechaSolicitud", desde, hasta, false);
    }

    /** Suma de los pagos APROBADOS en el rango. */
    public BigDecimal sumarPagosAprobados(LocalDateTime desde, LocalDateTime hasta) {
        Filtro f = new Filtro();
        f.rango("x.fechaPago", desde, hasta);
        f.condicion("x.estado = :estadoPago", "estadoPago", "APROBADO");
        Query q = em.createQuery("SELECT COALESCE(SUM(x.monto), 0) FROM PagoEntity x" + f.where());
        f.aplicar(q);
        Object resultado = q.getSingleResult();
        return resultado instanceof BigDecimal b ? b : new BigDecimal(String.valueOf(resultado));
    }

    // ---- Utilidades internas ----

    private long contar(String entidad, String campo, LocalDateTime desde, LocalDateTime hasta, boolean soloFecha) {
        Filtro f = new Filtro();
        if (soloFecha) {
            f.rango("x." + campo, desde != null ? desde.toLocalDate() : null, hasta != null ? hasta.toLocalDate() : null);
        } else {
            f.rango("x." + campo, desde, hasta);
        }
        Query q = em.createQuery("SELECT COUNT(x) FROM " + entidad + " x" + f.where());
        f.aplicar(q);
        return ((Number) q.getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> ejecutar(String jpql, Filtro filtro) {
        Query q = em.createQuery(jpql);
        filtro.aplicar(q);
        return q.getResultList();
    }

    /** Arma el WHERE dinámico solo con los filtros presentes. */
    private static class Filtro {
        private final StringBuilder where = new StringBuilder();
        private final Map<String, Object> parametros = new HashMap<>();

        void rango(String campo, Object desde, Object hasta) {
            if (desde != null) {
                condicion(campo + " >= :desde", "desde", desde);
            }
            if (hasta != null) {
                condicion(campo + " < :hasta", "hasta", hasta);
            }
        }

        void sede(String campo, Long sedeId) {
            if (sedeId != null) {
                condicion(campo + " = :sede", "sede", sedeId);
            }
        }

        void condicion(String texto, String nombre, Object valor) {
            where.append(where.length() == 0 ? " WHERE " : " AND ").append(texto);
            parametros.put(nombre, valor);
        }

        String where() {
            return where.toString();
        }

        void aplicar(Query q) {
            parametros.forEach(q::setParameter);
        }
    }
}
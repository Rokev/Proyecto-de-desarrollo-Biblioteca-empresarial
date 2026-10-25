package bussinesLayer.util;

import java.util.List;

/**
 * Infracciones que pueden generar una sanción (RF-74), con la política por defecto
 * que se carga la primera vez que arranca el sistema.
 */
public enum InfraccionSancion {

    /** Préstamo vencido. Sin duración = la sanción dura hasta que el usuario devuelva el libro. */
    MORA_DEVOLUCION("Retraso en la devolucion de un libro", TipoSancion.BLOQUEO_PRESTAMOS, null, true),
    /** Pedido interciudad no recogido en la sede de devolución a tiempo (RF-76 c). */
    PEDIDO_NO_RECOGIDO("No recoger a tiempo un pedido interciudad", TipoSancion.BLOQUEO_PRESTAMOS, 7, true),
    /** Libro devuelto dañado (RF-41). Por defecto no genera sanción, solo multa. */
    LIBRO_DANADO("Devolver un libro dañado", TipoSancion.BLOQUEO_PRESTAMOS, 7, false),
    /** Libro perdido (RF-41). Por defecto no genera sanción, solo multa. */
    LIBRO_PERDIDO("Perder un libro", TipoSancion.BLOQUEO_PRESTAMOS, 30, false);

    private final String descripcion;
    private final String tipoPorDefecto;
    private final Integer duracionDiasPorDefecto;
    private final boolean activaPorDefecto;

    InfraccionSancion(String descripcion, String tipoPorDefecto, Integer duracionDiasPorDefecto, boolean activaPorDefecto) {
        this.descripcion = descripcion;
        this.tipoPorDefecto = tipoPorDefecto;
        this.duracionDiasPorDefecto = duracionDiasPorDefecto;
        this.activaPorDefecto = activaPorDefecto;
    }

    public String getDescripcion() { return descripcion; }
    public String getTipoPorDefecto() { return tipoPorDefecto; }
    public Integer getDuracionDiasPorDefecto() { return duracionDiasPorDefecto; }
    public boolean isActivaPorDefecto() { return activaPorDefecto; }

    /** Solo la mora puede tener duración indefinida (hasta devolver el libro). */
    public boolean permiteDuracionIndefinida() {
        return this == MORA_DEVOLUCION;
    }

    /** Tipos de sanción que se pueden configurar. */
    public static final class TipoSancion {
        public static final String BLOQUEO_PRESTAMOS = "BLOQUEO_PRESTAMOS";
        public static final String SUSPENSION_CUENTA = "SUSPENSION_CUENTA";
        public static final String ADVERTENCIA = "ADVERTENCIA";
        public static final List<String> TODOS = List.of(BLOQUEO_PRESTAMOS, SUSPENSION_CUENTA, ADVERTENCIA);

        private TipoSancion() {
        }
    }
}

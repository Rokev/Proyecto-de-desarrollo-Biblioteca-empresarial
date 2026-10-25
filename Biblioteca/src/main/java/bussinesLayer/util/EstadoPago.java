package bussinesLayer.util;

import java.util.List;

/** Estados válidos de un pago. */
public final class EstadoPago {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String APROBADO = "APROBADO";
    public static final String RECHAZADO = "RECHAZADO";
    public static final String REEMBOLSADO = "REEMBOLSADO";

    public static final List<String> TODOS = List.of(PENDIENTE, APROBADO, RECHAZADO, REEMBOLSADO);

    private EstadoPago() {
    }
}
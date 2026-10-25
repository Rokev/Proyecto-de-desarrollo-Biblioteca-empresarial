package bussinesLayer.util;

import java.math.BigDecimal;

/** Conceptos económicos configurables, con el monto por defecto que se carga al arrancar. */
public enum ConceptoMulta {

    MORA_POR_DIA("Multa por cada dia de retraso en la devolucion", "2000.00"),
    LIBRO_DANADO("Multa por devolver un libro dañado", "30000.00"),
    LIBRO_PERDIDO("Multa por perdida de un libro", "60000.00"),
    PEDIDO_NO_RECOGIDO("Multa por no recoger a tiempo un pedido interciudad en la sede de devolucion", "20000.00"),
    RECARGO_PEDIDO_NO_RECIBIDO("Recargo fijo por no recibir el pedido interciudad en la fecha y hora indicadas", "15000.00");

    private final String descripcion;
    private final BigDecimal montoPorDefecto;

    ConceptoMulta(String descripcion, String montoPorDefecto) {
        this.descripcion = descripcion;
        this.montoPorDefecto = new BigDecimal(montoPorDefecto);
    }

    public String getDescripcion() { return descripcion; }
    public BigDecimal getMontoPorDefecto() { return montoPorDefecto; }
}

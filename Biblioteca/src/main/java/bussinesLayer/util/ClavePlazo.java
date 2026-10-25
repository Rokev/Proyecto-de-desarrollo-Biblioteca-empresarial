package bussinesLayer.util;

/** Plazos configurables del sistema (RF-76), con su valor por defecto y rango permitido. */
public enum ClavePlazo {

    DIAS_PRESTAMO("Dias de prestamo de un libro (retiro en sede)", "DIAS", 14, 1, 90),
    DIAS_PRESTAMO_INTERCIUDAD("Dias para devolver un pedido interciudad, contados desde la entrega a domicilio o el retiro en la sede de devolucion",
            "DIAS", 14, 1, 90),
    HORAS_RETIRO_LIBRO("Horas para retirar un libro solicitado o reservado", "HORAS", 48, 1, 720),
    DIAS_RECOGIDA_SEDE_DEVOLUCION("Dias para recoger en la sede de devolucion un pedido interciudad no recibido", "DIAS", 2, 1, 30);

    private final String descripcion;
    private final String unidad;
    private final int valorPorDefecto;
    private final int minimo;
    private final int maximo;

    ClavePlazo(String descripcion, String unidad, int valorPorDefecto, int minimo, int maximo) {
        this.descripcion = descripcion;
        this.unidad = unidad;
        this.valorPorDefecto = valorPorDefecto;
        this.minimo = minimo;
        this.maximo = maximo;
    }

    public String getDescripcion() { return descripcion; }
    public String getUnidad() { return unidad; }
    public int getValorPorDefecto() { return valorPorDefecto; }
    public int getMinimo() { return minimo; }
    public int getMaximo() { return maximo; }
}

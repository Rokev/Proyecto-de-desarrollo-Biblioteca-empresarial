package bussinesLayer.exception;

/**
 * Se lanza cuando un recurso (cliente, viaje, reserva, transporte) no existe.
 * Se traduce a HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

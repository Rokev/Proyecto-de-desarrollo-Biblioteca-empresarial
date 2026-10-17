package bussinesLayer.exception;

/**
 * Se lanza cuando se incumple una regla de negocio (email duplicado, viaje inexistente
 * al reservar, fecha en el pasado, estado invalido, etc.). Se traduce a HTTP 400.
 */
public class BusinessValidationException extends RuntimeException {

    public BusinessValidationException(String message) {
        super(message);
    }
}

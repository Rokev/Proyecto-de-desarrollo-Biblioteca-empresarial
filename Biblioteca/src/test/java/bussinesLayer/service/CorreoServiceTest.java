package bussinesLayer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

@DisplayName("CorreoService")
class CorreoServiceTest {

    private static final String URL_FRONT = "http://localhost:5173";
    private static final String REMITENTE = "no-reply@libroya.com";

    private ObjectProvider<JavaMailSender> proveedor;
    private JavaMailSender sender;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        proveedor = Mockito.mock(ObjectProvider.class);
        sender = Mockito.mock(JavaMailSender.class);
    }

    @Test
    @DisplayName("sin servidor SMTP configurado no falla y no envia nada (modo desarrollo)")
    void sinSmtp_soloRegistraEnElLog() {
        Mockito.when(proveedor.getIfAvailable()).thenReturn(null);
        CorreoService service = new CorreoService(proveedor, URL_FRONT, REMITENTE);

        assertDoesNotThrow(() -> service.enviarRecuperacionContrasena("ana@correo.com", "tok-123"));

        Mockito.verifyNoInteractions(sender);
    }

    @Test
    @DisplayName("con SMTP envia un correo al usuario con el enlace que contiene el token")
    void conSmtp_enviaElEnlaceDeRecuperacion() {
        Mockito.when(proveedor.getIfAvailable()).thenReturn(sender);
        CorreoService service = new CorreoService(proveedor, URL_FRONT, REMITENTE);

        service.enviarRecuperacionContrasena("ana@correo.com", "tok-123");

        ArgumentCaptor<SimpleMailMessage> enviado = ArgumentCaptor.forClass(SimpleMailMessage.class);
        Mockito.verify(sender).send(enviado.capture());
        SimpleMailMessage mensaje = enviado.getValue();
        assertArrayEquals(new String[]{"ana@correo.com"}, mensaje.getTo());
        assertEquals(REMITENTE, mensaje.getFrom());
        assertNotNull(mensaje.getSubject());
        assertTrue(mensaje.getText().contains(URL_FRONT + "/restablecer-contrasena?token=tok-123"));
        assertTrue(mensaje.getText().contains("24 horas"));
    }

    @Test
    @DisplayName("si el servidor de correo falla no se propaga el error (no revela si el correo existe)")
    void falloDeEnvio_noSePropaga() {
        Mockito.when(proveedor.getIfAvailable()).thenReturn(sender);
        Mockito.doThrow(new MailSendException("SMTP caido")).when(sender).send(any(SimpleMailMessage.class));
        CorreoService service = new CorreoService(proveedor, URL_FRONT, REMITENTE);

        assertDoesNotThrow(() -> service.enviarRecuperacionContrasena("ana@correo.com", "tok-123"));
    }
}

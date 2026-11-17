package bussinesLayer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Envia correos. Si no hay servidor SMTP configurado (desarrollo), el enlace solo se escribe en el log. */
@Service
@Slf4j
public class CorreoService {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String urlFrontend;
    private final String remitente;

    public CorreoService(ObjectProvider<JavaMailSender> mailSender,
                         @Value("${app.frontend.url}") String urlFrontend,
                         @Value("${spring.mail.username:no-reply@libroya.com}") String remitente) {
        this.mailSender = mailSender;
        this.urlFrontend = urlFrontend;
        this.remitente = remitente;
    }

    public void enviarRecuperacionContrasena(String correo, String token) {
        String enlace = urlFrontend + "/restablecer-contrasena?token=" + token;
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("(Sin SMTP configurado) Enlace de recuperacion para {}: {}", correo, enlace);
            return;
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(correo);
            mensaje.setSubject("Recupera tu contrasena - LibrosYa");
            mensaje.setText("Recibimos una solicitud para restablecer tu contrasena.\n\n"
                    + "Usa este enlace (vence en 24 horas y solo funciona una vez):\n" + enlace + "\n\n"
                    + "Si no fuiste tu, ignora este mensaje.");
            sender.send(mensaje);
            log.info("Correo de recuperacion enviado a {}", correo);
        } catch (Exception e) {
            // No se propaga: la respuesta del endpoint no debe revelar si el correo existe
            log.error("No se pudo enviar el correo de recuperacion a {}: {}", correo, e.getMessage());
        }
    }
}
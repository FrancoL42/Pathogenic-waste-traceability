package ar.edu.utn.frc.tup.lciii.services.impl;

import ar.edu.utn.frc.tup.lciii.services.EnvioCorreosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EnvioCorreosServiceImpl implements EnvioCorreosService {

    @Autowired
    private JavaMailSender mailSender;

    private final String emailFrom = "enviocorreos12345@gmail.com";

    @Override
    public void enviarCorreo(String para, String asunto, String cuerpo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(para);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);
        mensaje.setFrom(emailFrom);

        mailSender.send(mensaje);
    }
    @Override
    public void enviarCorreoConAdjunto(String para, String asunto, String cuerpo, String nombreAdjunto, byte[] contenidoAdjunto) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(para);
            helper.setSubject(asunto);
            helper.setText(cuerpo);
            helper.setFrom(emailFrom);

            // Agregar adjunto
            helper.addAttachment(nombreAdjunto, new ByteArrayResource(contenidoAdjunto));

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error enviando correo con adjunto", e);
        }
    }

    @Override
    public void enviarCorreoConAdjuntoRecurso(String para, String asunto, String cuerpo, String nombreAdjunto, Resource recurso) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(para);
            helper.setSubject(asunto);
            helper.setText(cuerpo);
            helper.setFrom(emailFrom);

            // Agregar adjunto desde recurso
            helper.addAttachment(nombreAdjunto, recurso);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error enviando correo con adjunto", e);
        }
    }
}

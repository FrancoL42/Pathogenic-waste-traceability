package ar.edu.utn.frc.tup.lciii.config;

import ar.edu.utn.frc.tup.lciii.services.CertificateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Slf4j
public class CertificateScheduler {

    @Autowired
    private CertificateService certificateService;

    @Scheduled(cron = "0 0 23 * * *") // Todos los días a las 23:00
    public void generarCertificadosDiarios() {
        try {
            certificateService.generarCertificadosDelDia(LocalDate.now());
            log.info("Certificados diarios generados exitosamente");
        } catch (Exception e) {
            log.error("Error generando certificados diarios: ", e);
        }
    }
}
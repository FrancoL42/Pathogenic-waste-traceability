package ar.edu.utn.frc.tup.lciii.services;

import ar.edu.utn.frc.tup.lciii.entities.CertificateEntity;

import java.time.LocalDate;
import java.util.List;

public interface CertificateService {

    void generarCertificadosDelDia(LocalDate fecha);

    void generarCertificadoManual(Long generadorId, LocalDate fecha);
}
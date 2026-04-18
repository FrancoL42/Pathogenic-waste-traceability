package ar.edu.utn.frc.tup.lciii.repositories;

import ar.edu.utn.frc.tup.lciii.entities.CertificateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<CertificateEntity, Long> {
    List<CertificateEntity> findByGeneratorEntityGeneratorIdOrderByFechaGeneracionDesc(Long generadorId);
    Optional<CertificateEntity> findByNumeroCertificado(String numeroCertificado);
}

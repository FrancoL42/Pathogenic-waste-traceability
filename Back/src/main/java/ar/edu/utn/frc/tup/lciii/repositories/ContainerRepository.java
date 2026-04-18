package ar.edu.utn.frc.tup.lciii.repositories;

import ar.edu.utn.frc.tup.lciii.entities.ContainersEntity;
import ar.edu.utn.frc.tup.lciii.models.ContainerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContainerRepository extends JpaRepository<ContainersEntity, Long> {
    List<ContainersEntity> findByEstado(ContainerStatus estado);

    List<ContainersEntity> findByEstadoOrderByFechaCreacionDesc(ContainerStatus estado);

    // Método opcional para obtener contenedores ordenados por fecha
    List<ContainersEntity> findAllByOrderByFechaCreacionDesc();
}

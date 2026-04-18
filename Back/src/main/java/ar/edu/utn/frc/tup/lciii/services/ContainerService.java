package ar.edu.utn.frc.tup.lciii.services;

import ar.edu.utn.frc.tup.lciii.entities.ContainersEntity;
import ar.edu.utn.frc.tup.lciii.models.ContainerStatus;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ContainerService {
    ContainersEntity crearContenedor(Double pesoMaximo);

    void agregarPrecintosAContenedor(Long containerId, List<Long> sealIds);

    void cerrarContenedor(Long containerId);

    void abrirContenedorTratado(Long containerId);

    Double consultarPesoDisponible(Long containerId);

    List<ContainersEntity> listarTodos();

    List<ContainersEntity> listarPorEstado(ContainerStatus estado);

    @Transactional
    void removerPrecintosDeContenedor(Long containerId, List<Long> sealIds);
}

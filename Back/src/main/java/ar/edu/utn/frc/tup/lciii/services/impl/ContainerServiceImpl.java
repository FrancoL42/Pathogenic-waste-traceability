package ar.edu.utn.frc.tup.lciii.services.impl;

import ar.edu.utn.frc.tup.lciii.entities.ContainersEntity;
import ar.edu.utn.frc.tup.lciii.entities.SealEntity;
import ar.edu.utn.frc.tup.lciii.models.ContainerStatus;
import ar.edu.utn.frc.tup.lciii.models.SealStatus;
import ar.edu.utn.frc.tup.lciii.repositories.ContainerRepository;
import ar.edu.utn.frc.tup.lciii.repositories.SealRepository;
import ar.edu.utn.frc.tup.lciii.services.ContainerService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ContainerServiceImpl implements ContainerService {
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ContainerRepository containerRepository;
    @Autowired
    private SealRepository sealRepository;
    @Override
    public ContainersEntity crearContenedor(Double pesoMaximo) {
        ContainersEntity container = new ContainersEntity();
        container.setPesoMaximo(pesoMaximo);
        return containerRepository.save(container);
    }
    @Override
    // En ContainerService.java - modificar el método existente
    public void agregarPrecintosAContenedor(Long containerId, List<Long> sealIds) {
        ContainersEntity container = containerRepository.findById(containerId)
                .orElseThrow(() -> new EntityNotFoundException("Contenedor no encontrado"));

        if (container.getEstado() != ContainerStatus.ABIERTO) {
            throw new IllegalStateException("El contenedor no está abierto");
        }

        List<SealEntity> seals = sealRepository.findAllById(sealIds);

        // Validar que todos los precintos estén en estado RECOLECTADO
        seals.forEach(seal -> {
            if (seal.getState() != SealStatus.EN_PLANTA) {
                throw new IllegalStateException("Precinto " + seal.getQrContent() + " no está en estado RECOLECTADO");
            }

            if (seal.getPeso() == null || seal.getPeso() <= 0) {
                throw new IllegalStateException("El precinto " + seal.getQrContent() + " debe ser pesado antes de agregarlo al contenedor");
            }
        });

        Double pesoTotal = seals.stream()
                .mapToDouble(SealEntity::getPeso)
                .sum();

        if (container.getPesoActual() + pesoTotal > container.getPesoMaximo()) {
            throw new IllegalArgumentException("Peso máximo excedido. Peso disponible: " +
                    (container.getPesoMaximo() - container.getPesoActual()) + " kg");
        }

        // Cambiar estado de precintos y asociar al contenedor
        seals.forEach(seal -> {
            seal.setState(SealStatus.EN_TRATAMIENTO);
            seal.setContainersEntity(container);
        });

        container.setPesoActual(container.getPesoActual() + pesoTotal);
        container.getSeals().addAll(seals);

        // Auto-cerrar si alcanza peso máximo
        if (container.getPesoActual().equals(container.getPesoMaximo())) {
            cerrarContenedor(containerId);
        }

        containerRepository.save(container);
    }
    @Override
    public void cerrarContenedor(Long containerId) {
        ContainersEntity container = containerRepository.findById(containerId)
                .orElseThrow(() -> new EntityNotFoundException("Contenedor no encontrado"));

        if (container.getEstado() != ContainerStatus.ABIERTO) {
            throw new IllegalStateException("El contenedor ya está cerrado");
        }

        container.setEstado(ContainerStatus.CERRADO);
        containerRepository.save(container);
    }
    @Override
    public void abrirContenedorTratado(Long containerId) {
        ContainersEntity container = containerRepository.findById(containerId)
                .orElseThrow(() -> new EntityNotFoundException("Contenedor no encontrado"));

        if (container.getEstado() != ContainerStatus.CERRADO) {
            throw new IllegalStateException("El contenedor no está cerrado para tratamiento");
        }

        log.info("🔄 Procesando tratamiento del contenedor {}", containerId);
        log.info("📦 Precintos en el contenedor: {}", container.getSeals().size());
        log.info("⚖️ Peso actual del contenedor: {} kg", container.getPesoActual());

        // 🔧 PRIMERO: Crear una lista separada de los precintos para procesar
        List<SealEntity> sealsToProcess = new ArrayList<>(container.getSeals());

        // 🔧 SEGUNDO: Cambiar estado de precintos pero SIN desasociar todavía
        sealsToProcess.forEach(seal -> {
            log.info("🔄 Procesando precinto {}: {} -> TRATADO", seal.getSealId(), seal.getState());

            seal.setState(SealStatus.TRATADO);
            seal.setFechaTratamiento(LocalDate.now());

            log.info("✅ Precinto {} marcado como tratado", seal.getSealId());
        });

        // 🔧 TERCERO: Guardar explícitamente los precintos con sus nuevos estados
        sealRepository.saveAll(sealsToProcess);
        log.info("💾 {} precintos guardados con estado TRATADO", sealsToProcess.size());

        // 🔧 CUARTO: Ahora sí desasociar los precintos del contenedor
        sealsToProcess.forEach(seal -> {
            seal.setContainersEntity(null); // Desasociar del contenedor
            log.info("🔗 Precinto {} desasociado del contenedor", seal.getSealId());
        });

        // 🔧 QUINTO: Guardar nuevamente los precintos desasociados
        sealRepository.saveAll(sealsToProcess);
        log.info("💾 {} precintos desasociados y guardados", sealsToProcess.size());

        // 🔧 SEXTO: Resetear el contenedor para reutilización
        container.setEstado(ContainerStatus.ABIERTO);  // Volver a ABIERTO
        container.setPesoActual(0.0);                  // Resetear peso a 0
        container.getSeals().clear();                  // Limpiar la lista de precintos

        log.info("🔄 Contenedor {} reseteado:", containerId);
        log.info("   - Estado: {} -> ABIERTO", ContainerStatus.CERRADO);
        log.info("   - Peso: resetado a 0 kg");
        log.info("   - Precintos: liberados");

        // 🔧 SÉPTIMO: Guardar el contenedor reseteado
        containerRepository.save(container);

        log.info("✅ Tratamiento completado. Contenedor {} listo para reutilización", containerId);
        log.info("✅ {} precintos procesados y listos como TRATADOS", sealsToProcess.size());
    }
    @Override
    public Double consultarPesoDisponible(Long containerId) {
        ContainersEntity container = containerRepository.findById(containerId)
                .orElseThrow(() -> new EntityNotFoundException("Contenedor no encontrado"));

        return container.getPesoMaximo() - container.getPesoActual();
    }

    @Override
    public List<ContainersEntity> listarTodos() {
        try {
            log.debug("Obteniendo todos los contenedores");
            List<ContainersEntity> containers = containerRepository.findAll();
            log.info("Se encontraron {} contenedores", containers.size());
            return containers;
        } catch (Exception e) {
            log.error("Error obteniendo todos los contenedores", e);
            throw new RuntimeException("Error consultando contenedores", e);
        }
    }
    @Override
    public List<ContainersEntity> listarPorEstado(ContainerStatus estado) {
        try {
            log.debug("Obteniendo contenedores con estado: {}", estado);
            List<ContainersEntity> containers = containerRepository.findByEstado(estado);
            log.info("Se encontraron {} contenedores con estado {}", containers.size(), estado);
            return containers;
        } catch (Exception e) {
            log.error("Error obteniendo contenedores por estado: " + estado, e);
            throw new RuntimeException("Error consultando contenedores por estado", e);
        }
    }
    @Override
    @Transactional
    public void removerPrecintosDeContenedor(Long containerId, List<Long> sealIds) {
        log.info("🔍 Iniciando remoción: contenedor {}, precintos {}", containerId, sealIds);

        try {
            ContainersEntity container = containerRepository.findById(containerId)
                    .orElseThrow(() -> new EntityNotFoundException("Contenedor no encontrado"));

            log.info("🔍 Contenedor encontrado: estado={}", container.getEstado());

            if (container.getEstado() != ContainerStatus.ABIERTO) {
                throw new IllegalStateException("Solo se pueden remover precintos de contenedores abiertos");
            }

            List<SealEntity> seals = sealRepository.findAllById(sealIds);
            log.info("🔍 Precintos encontrados: {}", seals.size());

            // Validar que los precintos estén en este contenedor
            for (SealEntity seal : seals) {
                log.info("🔍 Validando precinto {}: contenedor actual={}, estado={}",
                        seal.getSealId(),
                        seal.getContainersEntity() != null ? seal.getContainersEntity().getContainerId() : "null",
                        seal.getState());

                if (seal.getContainersEntity() == null ||
                        !seal.getContainersEntity().getContainerId().equals(containerId)) {
                    throw new IllegalStateException("Precinto " + seal.getQrContent() +
                            " no está en este contenedor");
                }

                if (seal.getState() != SealStatus.EN_TRATAMIENTO) {
                    throw new IllegalStateException("Precinto " + seal.getQrContent() +
                            " no está en tratamiento. Estado: " + seal.getState());
                }
            }

            // Calcular peso a remover
            Double pesoARemover = seals.stream().mapToDouble(SealEntity::getPeso).sum();
            log.info("🔍 Peso a remover: {} kg", pesoARemover);

            // Cambiar estados de precintos
            for (SealEntity seal : seals) {
                seal.setState(SealStatus.EN_PLANTA);
                seal.setContainersEntity(null);
                log.info("🔄 Precinto {} cambiado a RECOLECTADO", seal.getSealId());
            }

            // Actualizar peso del contenedor
            container.setPesoActual(container.getPesoActual() - pesoARemover);
            log.info("🔄 Peso contenedor actualizado: {} kg", container.getPesoActual());

            // Guardar cambios
            sealRepository.saveAll(seals);
            containerRepository.save(container);

            log.info("✅ Precintos removidos exitosamente");

        } catch (Exception e) {
            log.error("❌ Error removiendo precintos: {}", e.getMessage(), e);
            throw e; // Re-lanzar para que llegue al controller
        }
    }
}

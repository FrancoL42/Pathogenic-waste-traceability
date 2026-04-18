package ar.edu.utn.frc.tup.lciii.services.impl;

import ar.edu.utn.frc.tup.lciii.dtos.ContainerDTO;
import ar.edu.utn.frc.tup.lciii.dtos.GeneratorDto;
import ar.edu.utn.frc.tup.lciii.dtos.SealInfo;
import ar.edu.utn.frc.tup.lciii.dtos.SealTreatmentInfo;
import ar.edu.utn.frc.tup.lciii.entities.ContainersEntity;
import ar.edu.utn.frc.tup.lciii.entities.GeneratorEntity;
import ar.edu.utn.frc.tup.lciii.entities.SealEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DTOMapperService {

    // ===== SEAL MAPPING =====

    public SealTreatmentInfo convertSealToTreatmentInfo(SealEntity seal) {
        if (seal == null) return null;

        SealTreatmentInfo dto = new SealTreatmentInfo();
        dto.setSealId(seal.getSealId());
        dto.setSealNumber(seal.getSealNumber());
        dto.setState(seal.getState() != null ? seal.getState().toString() : null);
        dto.setQrContent(seal.getQrContent());
        dto.setPeso(seal.getPeso());
        dto.setFechaTratamiento(seal.getFechaTratamiento());

        // Mapear generador
        if (seal.getGeneratorEntity() != null) {
            dto.setGeneratorEntity(convertGeneratorToDto(seal.getGeneratorEntity()));
        }

        // Solo el ID del contenedor
        if (seal.getContainersEntity() != null) {
            dto.setContainerId(seal.getContainersEntity().getContainerId());
        }

        return dto;
    }

    public List<SealTreatmentInfo> convertSealsToTreatmentInfo(List<SealEntity> seals) {
        if (seals == null) return Collections.emptyList();
        return seals.stream()
                .map(this::convertSealToTreatmentInfo)
                .collect(Collectors.toList());
    }

    public SealInfo convertSealToInfo(SealEntity seal) {
        if (seal == null) return null;

        SealInfo dto = new SealInfo();
        dto.setSealId(seal.getSealId());
        dto.setSealNumber(seal.getSealNumber());
        dto.setState(seal.getState() != null ? seal.getState().toString() : null);
        dto.setQrContent(seal.getQrContent());

        return dto;
    }

    // ===== CONTAINER MAPPING =====

    public ContainerDTO convertContainerToDTO(ContainersEntity container) {
        if (container == null) return null;

        ContainerDTO dto = new ContainerDTO();
        dto.setContainerId(container.getContainerId());
        dto.setPesoMaximo(container.getPesoMaximo());
        dto.setPesoActual(container.getPesoActual());
        dto.setEstado(container.getEstado() != null ? container.getEstado().toString() : null);
        dto.setFechaCreacion(container.getFechaCreacion());


        // 🆕 MAPEAR PRECINTOS DEL CONTENEDOR
        if (container.getSeals() != null && !container.getSeals().isEmpty()) {
            List<SealTreatmentInfo> sealsDTO = container.getSeals().stream()
                    .map(seal -> {
                        SealTreatmentInfo sealInfo = new SealTreatmentInfo();
                        sealInfo.setSealId(seal.getSealId());
                        sealInfo.setSealNumber(seal.getSealNumber());
                        sealInfo.setState(seal.getState() != null ? seal.getState().toString() : null);
                        sealInfo.setQrContent(seal.getQrContent());
                        sealInfo.setPeso(seal.getPeso());
                        sealInfo.setFechaTratamiento(seal.getFechaTratamiento());

                        // Mapear generador
                        if (seal.getGeneratorEntity() != null) {
                            sealInfo.setGeneratorEntity(convertGeneratorToDto(seal.getGeneratorEntity()));
                        }

                        sealInfo.setContainerId(container.getContainerId());
                        return sealInfo;
                    })
                    .collect(Collectors.toList());

            dto.setSeals(sealsDTO);
            dto.setCantidadPrecintos(sealsDTO.size());
        } else {
            dto.setSeals(Collections.emptyList());
            dto.setCantidadPrecintos(0);
        }

        return dto;
    }

    public List<ContainerDTO> convertContainersToDTO(List<ContainersEntity> containers) {
        if (containers == null) return Collections.emptyList();
        return containers.stream()
                .map(this::convertContainerToDTO)
                .collect(Collectors.toList());
    }

    // ===== GENERATOR MAPPING =====

    public GeneratorDto convertGeneratorToDto(GeneratorEntity generator) {
        if (generator == null) return null;

        GeneratorDto dto = new GeneratorDto();
        dto.setGeneratorId(generator.getGeneratorId());
        dto.setName(generator.getName());
        dto.setEmail(generator.getEmail());
        dto.setContact(generator.getContact());
        dto.setType(generator.getType());
        dto.setAddress(generator.getAddress());
        dto.setLatitude(generator.getLatitude());
        dto.setLongitude(generator.getLongitude());
        dto.setState(generator.getState() != null ? generator.getState().toString() : null);

        // Zona (si existe)
        if (generator.getZone() != null) {
            dto.setZona(generator.getZone().getName()); // Asumiendo que ZoneEntity tiene un campo name
        }

        return dto;
    }
}
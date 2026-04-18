package ar.edu.utn.frc.tup.lciii.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContainerDTO {
    private Long containerId;
    private Double pesoMaximo;
    private Double pesoActual;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaCierre;
    private LocalDateTime fechaAperturaPostTratamiento;
    private List<SealTreatmentInfo> seals;
    private Integer cantidadPrecintos;
}
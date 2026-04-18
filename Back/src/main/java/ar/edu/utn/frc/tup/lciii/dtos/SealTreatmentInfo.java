package ar.edu.utn.frc.tup.lciii.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class SealTreatmentInfo extends SealInfo {
    private Double peso;
    private LocalDate fechaTratamiento;
    private GeneratorDto generatorEntity;
    private Long containerId;

    public SealTreatmentInfo(Long sealId, String sealNumber, String state, String qrContent,
                             Double peso, LocalDate fechaTratamiento, GeneratorDto generatorEntity, Long containerId) {
        super(sealId, sealNumber, state, qrContent);
        this.peso = peso;
        this.fechaTratamiento = fechaTratamiento;
        this.generatorEntity = generatorEntity;
        this.containerId = containerId;
    }
}
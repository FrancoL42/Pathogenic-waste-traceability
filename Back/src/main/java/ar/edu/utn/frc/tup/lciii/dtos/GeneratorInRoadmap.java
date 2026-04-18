package ar.edu.utn.frc.tup.lciii.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeneratorInRoadmap {
    private Long generatorId;
    private String name;
    private String address;
    private String status; // PENDIENTE, EN_PROCESO, COMPLETADO
    private Integer totalBags;
    private Integer requestedBags;
    private Integer collectedBags;
    private Double latitude;
    private Double longitude;
    private List<SealInfo> seals;
    private Long orderId;
}

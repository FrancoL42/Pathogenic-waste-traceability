package ar.edu.utn.frc.tup.lciii.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoadmapCloseRequest {
    private Long roadmapId;
    private LocalTime returnHour;
    private Double finalKm;
    private String observations;
}

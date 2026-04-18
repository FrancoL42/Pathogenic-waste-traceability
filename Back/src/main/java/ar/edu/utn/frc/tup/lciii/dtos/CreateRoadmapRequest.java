package ar.edu.utn.frc.tup.lciii.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateRoadmapRequest {
    private String zone;
    private String employee;
    private String collectDate; // "2025-05-28T12:00:00"
    private String exitHour;    // "12:00:00"
    private List<Long> selectedOrderIds;
    private Long vehicleId; // Opcional
}

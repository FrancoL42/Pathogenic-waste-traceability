package ar.edu.utn.frc.tup.lciii.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteOptimizationRequest {
    private Long roadmapId;
    private double startLatitude;
    private double startLongitude;
    private String startTime; // HH:mm
    private boolean includeReturnToDepot;
    private String optimizationMethod; // "NEAREST_NEIGHBOR", "TIME_WINDOWS", "DISTANCE"
}

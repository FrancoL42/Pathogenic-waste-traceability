package ar.edu.utn.frc.tup.lciii.dtos;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptimizedRouteResponse {
    private Long roadmapId;
    private String zone;
    private List<RouteWaypoint> waypoints;
    private RouteSummary summary;
    private String status;
}

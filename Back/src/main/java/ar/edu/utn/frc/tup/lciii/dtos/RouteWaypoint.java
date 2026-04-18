package ar.edu.utn.frc.tup.lciii.dtos;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteWaypoint {
    private int order;
    private Long generatorId;
    private String generatorName;
    private String address;
    private double latitude;
    private double longitude;
    private int estimatedBags;
    private int estimatedTimeMinutes;
    private double distanceFromPrevious;
    private String arrivalTime;
    private String status; // PENDING, VISITED, SKIPPED
}

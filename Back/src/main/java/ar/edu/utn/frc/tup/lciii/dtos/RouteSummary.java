package ar.edu.utn.frc.tup.lciii.dtos;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteSummary {
    private double totalDistance; // en kilómetros
    private int totalTimeMinutes; // tiempo estimado total
    private int totalGenerators;
    private int totalBags;
    private String startTime;
    private String estimatedEndTime;
    private String optimizationMethod;
}

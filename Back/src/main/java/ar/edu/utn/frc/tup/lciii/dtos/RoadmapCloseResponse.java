package ar.edu.utn.frc.tup.lciii.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoadmapCloseResponse {
    private Boolean success;
    private String message;
    private Long roadmapId;
    private Integer sealsDelivered;
    private Double kmTraveled;
    private Integer totalGenerators;
    private Long workDurationMinutes;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closeDateTime;

    // Métodos utilitarios
    public String getFormattedWorkDuration() {
        if (workDurationMinutes == null) return "N/A";

        long hours = workDurationMinutes / 60;
        long minutes = workDurationMinutes % 60;

        if (hours > 0) {
            return String.format("%d horas %d minutos", hours, minutes);
        } else {
            return String.format("%d minutos", minutes);
        }
    }

    public String getFormattedKm() {
        if (kmTraveled == null) return "N/A";
        return String.format("%.1f km", kmTraveled);
    }
}

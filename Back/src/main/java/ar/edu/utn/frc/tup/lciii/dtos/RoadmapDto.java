package ar.edu.utn.frc.tup.lciii.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoadmapDto {
    // Campos básicos
    private Long roadmapId;
    private LocalDateTime date;
    private String zone;
    private LocalDateTime collectDate;
    private String employee;
    private String vehicle; // Opcional por ahora

    // Campos de gestión de ruta
    private Double initialKm;
    private Double finalKm;
    private LocalTime exitHour;
    private LocalTime returnHour;
    private LocalTime collectHour;
    private Integer countBags;

    // Estado calculado
    private String state; // PENDIENTE, EN_PROCESO, COMPLETADA

    // Detalles de la hoja de ruta
    private List<RoadmapDetailDto> details;
}

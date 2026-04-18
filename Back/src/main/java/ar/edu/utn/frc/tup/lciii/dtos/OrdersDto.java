package ar.edu.utn.frc.tup.lciii.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrdersDto {
    private Long id;
    private String state;
    private String generador;
    private String address;
    private Double latitude;
    private Double longitude;

    // Información del empleado
    private Long employeeId;
    private String employeeName;
    private String employeeState;

    // Campos adicionales
    private String zone;
    private LocalDateTime creationDate;
    private LocalDateTime scheduledDate;
    private LocalDateTime completedDate;
    private Integer priority;
    private String priorityText;
    private String notes;

    // Campo legacy (mantener por compatibilidad)
    private String assignedEmployee;

    // Campos calculados para geolocalización
    private Double distanceFromEmployee;
    private Boolean hasValidCoordinates;
    private String contact;
    private String email;
    private String generatorType;

    // Constructor para mapeo básico

    // Métodos de utilidad
    public Boolean getHasValidCoordinates() {
        return latitude != null && longitude != null &&
                latitude >= -90 && latitude <= 90 &&
                longitude >= -180 && longitude <= 180;
    }

    public boolean isCompleted() {
        return "COMPLETADO".equalsIgnoreCase(state);
    }

    public boolean isPending() {
        return "PENDIENTE".equalsIgnoreCase(state);
    }

    public boolean isInProgress() {
        return "EN_PROCESO".equalsIgnoreCase(state);
    }

    public boolean hasAssignedEmployee() {
        return employeeId != null || (assignedEmployee != null && !assignedEmployee.trim().isEmpty());
    }

    public String getEffectiveEmployeeName() {
        return employeeName != null ? employeeName : assignedEmployee;
    }

    public boolean isEmployeeActive() {
        return "ACTIVO".equalsIgnoreCase(employeeState);
    }
}

package ar.edu.utn.frc.tup.lciii.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeLocationUpdate {
    private Long employeeId;
    private Long roadmapId;
    private double latitude;
    private double longitude;
    private String timestamp;
    private String status; // "TRAVELING", "AT_GENERATOR", "COLLECTING"
}

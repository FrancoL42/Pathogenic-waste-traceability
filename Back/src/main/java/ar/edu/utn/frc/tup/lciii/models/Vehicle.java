package ar.edu.utn.frc.tup.lciii.models;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Vehicle {
    private Long vehicleId;
    private String patent;
    private String type;
}

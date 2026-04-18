package ar.edu.utn.frc.tup.lciii.services;

import ar.edu.utn.frc.tup.lciii.models.Employee;
import ar.edu.utn.frc.tup.lciii.models.Vehicle;
import org.springframework.stereotype.Service;

@Service
public interface VehicleService {
    Vehicle registerVehicle(Vehicle v);
    Vehicle updateVehicle(Vehicle v);
    Vehicle deleteVehicle(Long id);
}

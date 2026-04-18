package ar.edu.utn.frc.tup.lciii.services.impl;

import ar.edu.utn.frc.tup.lciii.entities.EmployeesEntity;
import ar.edu.utn.frc.tup.lciii.entities.VehiclesEntity;
import ar.edu.utn.frc.tup.lciii.models.Employee;
import ar.edu.utn.frc.tup.lciii.models.Vehicle;
import ar.edu.utn.frc.tup.lciii.repositories.VehicleRepository;
import ar.edu.utn.frc.tup.lciii.services.VehicleService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VehicleServiceImpl implements VehicleService {
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private VehicleRepository repository;
    @Override
    public Vehicle registerVehicle(Vehicle v) {
        if(repository.existsByPatent(v.getPatent())){
            throw new IllegalArgumentException("Este vehiculo ya existe");
        }
        VehiclesEntity entity = modelMapper.map(v, VehiclesEntity.class);
        VehiclesEntity entitySaved = repository.save(entity);
        return modelMapper.map(entitySaved, Vehicle.class);
    }

    @Override
    public Vehicle updateVehicle(Vehicle v) {
        return null;
    }

    @Override
    public Vehicle deleteVehicle(Long id) {
        return null;
    }
}

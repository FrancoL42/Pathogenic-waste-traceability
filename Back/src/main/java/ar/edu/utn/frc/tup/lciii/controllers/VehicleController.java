package ar.edu.utn.frc.tup.lciii.controllers;

import ar.edu.utn.frc.tup.lciii.dtos.EmployeeDto;
import ar.edu.utn.frc.tup.lciii.dtos.VehicleDto;
import ar.edu.utn.frc.tup.lciii.models.Employee;
import ar.edu.utn.frc.tup.lciii.models.Vehicle;
import ar.edu.utn.frc.tup.lciii.services.VehicleService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:4200")

@RestController
@RequestMapping("/vehicle")
public class VehicleController {
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private VehicleService vehicleService;
    @PostMapping("/register")
    public ResponseEntity<VehicleDto> registerVehicle(@RequestBody VehicleDto e) {
        Vehicle v = vehicleService.registerVehicle(modelMapper.map(e, Vehicle.class));
        return ResponseEntity.ok(modelMapper.map(v, VehicleDto.class));
    }
}

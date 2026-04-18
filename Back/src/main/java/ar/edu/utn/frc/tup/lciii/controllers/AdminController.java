package ar.edu.utn.frc.tup.lciii.controllers;

import ar.edu.utn.frc.tup.lciii.dtos.EmployeeDto;
import ar.edu.utn.frc.tup.lciii.dtos.OrdersDto;
import ar.edu.utn.frc.tup.lciii.dtos.RoadmapDto;
import ar.edu.utn.frc.tup.lciii.dtos.StockDto;
import ar.edu.utn.frc.tup.lciii.dtos.VehicleDto;
import ar.edu.utn.frc.tup.lciii.dtos.ZoneDto;
import ar.edu.utn.frc.tup.lciii.entities.EmployeesEntity;
import ar.edu.utn.frc.tup.lciii.entities.OrdersEntity;
import ar.edu.utn.frc.tup.lciii.entities.RoadmapEntity;
import ar.edu.utn.frc.tup.lciii.entities.VehiclesEntity;
import ar.edu.utn.frc.tup.lciii.entities.ZoneEntity;
import ar.edu.utn.frc.tup.lciii.services.AdminNeedsService;
import ar.edu.utn.frc.tup.lciii.services.StockService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")

@RestController
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private AdminNeedsService adminNeedsService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private StockService stockService;
    @GetMapping("/zones")
    public ResponseEntity<List<ZoneDto>> getZones(){
        List<ZoneEntity> zoneEntities = adminNeedsService.getZones();
        List<ZoneDto> zoneDtos = new ArrayList<>();
        for (ZoneEntity zoneEntity : zoneEntities) {
            zoneDtos.add(modelMapper.map(zoneEntity, ZoneDto.class));
        }
        return ResponseEntity.ok(zoneDtos);
    }
    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeDto>> getEmployees(){
        List<EmployeesEntity> employees = adminNeedsService.getEmployees();
        List<EmployeeDto> employeeDtos = new ArrayList<>();
        for (EmployeesEntity entity : employees) {
            employeeDtos.add(modelMapper.map(entity, EmployeeDto.class));
        }
        return ResponseEntity.ok(employeeDtos);
    }
    @GetMapping("/vehicles")
    public ResponseEntity<List<VehicleDto>> getVehicles(){
        List<VehiclesEntity> vehiclesEntities = adminNeedsService.getVehicles();
        List<VehicleDto> vehicleDtos = new ArrayList<>();
        for (VehiclesEntity entity : vehiclesEntities) {
            vehicleDtos.add(modelMapper.map(entity, VehicleDto.class));
        }
        return ResponseEntity.ok(vehicleDtos);
    }
    @GetMapping("/pedidos")
    public ResponseEntity<List<OrdersDto>> getOrderByZone(@RequestParam String zone) {
        List<OrdersEntity> ordersEntities = adminNeedsService.getOrdersByZone(zone);
        List<OrdersDto> ordersDtos = new ArrayList<>();

        for (OrdersEntity entity : ordersEntities) {
            OrdersDto ordersDto = mapToDto(entity);
            ordersDtos.add(ordersDto);
        }

        return ResponseEntity.ok(ordersDtos);
    }

    // Método helper para mapeo manual
    private OrdersDto mapToDto(OrdersEntity entity) {
        OrdersDto dto = new OrdersDto();

        // Mapeo directo de campos básicos
        dto.setId(entity.getId());
        dto.setState(entity.getState());
        dto.setScheduledDate(entity.getScheduledDate());

        // Mapeo de GeneratorEntity
        if (entity.getGeneratorEntity() != null) {
            dto.setGenerador(entity.getGeneratorEntity().getName());
            dto.setAddress(entity.getGeneratorEntity().getAddress());
            dto.setLatitude(entity.getGeneratorEntity().getLatitude());
            dto.setLongitude(entity.getGeneratorEntity().getLongitude());
            dto.setContact(entity.getGeneratorEntity().getContact());
            dto.setEmail(entity.getGeneratorEntity().getEmail());
            dto.setGeneratorType(entity.getGeneratorEntity().getType());
        }

        // Mapeo de ZoneEntity
        if (entity.getZoneEntity() != null) {
            dto.setZone(entity.getZoneEntity().getName());
        }

        // Mapeo de EmployeeEntity
        if (entity.getEmployeeEntity() != null) {
            dto.setEmployeeId(entity.getEmployeeEntity().getId());
            dto.setEmployeeName(entity.getEmployeeEntity().getName());
            dto.setEmployeeState(entity.getEmployeeEntity().getState());
        }

        return dto;
    }
    @GetMapping("/stock")
    public ResponseEntity<List<StockDto>> getStock() {
        List<StockDto> stock = stockService.getAllStock();
        return ResponseEntity.ok(stock);
    }

    @GetMapping("/stock/low")
    public ResponseEntity<List<StockDto>> getLowStock() {
        List<StockDto> lowStock = stockService.getLowStock();
        return ResponseEntity.ok(lowStock);
    }

    @PostMapping("/solicitar-reposicion/{bagId}")
    public ResponseEntity<String> solicitarReposicion(@PathVariable Long bagId) {
        try {
            stockService.requestRestock(bagId);
            return ResponseEntity.ok("Solicitud de reposición enviada exitosamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al solicitar reposición: " + e.getMessage());
        }
    }

    @PostMapping("/stock/increment/{bagId}")
    public ResponseEntity<String> incrementStock(@PathVariable Long bagId, @RequestParam Integer quantity) {
        try {
            stockService.incrementStock(bagId, quantity);
            return ResponseEntity.ok("Stock incrementado exitosamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al incrementar stock: " + e.getMessage());
        }
    }
}

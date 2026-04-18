package ar.edu.utn.frc.tup.lciii.controllers;

import ar.edu.utn.frc.tup.lciii.dtos.EmployeeDto;
import ar.edu.utn.frc.tup.lciii.dtos.OrdersDto;
import ar.edu.utn.frc.tup.lciii.dtos.RoadmapCloseRequest;
import ar.edu.utn.frc.tup.lciii.dtos.RoadmapCloseResponse;
import ar.edu.utn.frc.tup.lciii.dtos.ScanQRRequest;
import ar.edu.utn.frc.tup.lciii.entities.EmployeesEntity;
import ar.edu.utn.frc.tup.lciii.entities.OrdersEntity;
import ar.edu.utn.frc.tup.lciii.entities.UserEntity;
import ar.edu.utn.frc.tup.lciii.models.Employee;
import ar.edu.utn.frc.tup.lciii.repositories.OrdersRepository;
import ar.edu.utn.frc.tup.lciii.repositories.RoadmapRepository;
import ar.edu.utn.frc.tup.lciii.repositories.UserRepository;
import ar.edu.utn.frc.tup.lciii.services.AdminNeedsService;
import ar.edu.utn.frc.tup.lciii.services.EmployeeService;
import ar.edu.utn.frc.tup.lciii.services.GeocodingService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:4200")

@RestController
@RequestMapping("/employee")
@Slf4j
public class EmployeeController {
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private RoadmapRepository roadmapRepository;
    @PostMapping("/register")
    public ResponseEntity<EmployeeDto> registerEmployee(@RequestBody EmployeeDto e) {
        Employee employee = employeeService.registerEmployee(modelMapper.map(e,Employee.class), e.getEmail());
        return ResponseEntity.ok(modelMapper.map(employee, EmployeeDto.class));
    }
    @PutMapping("/update")
    public ResponseEntity<EmployeeDto> updateEmployee(@RequestBody EmployeeDto e) {
        Employee employee = employeeService.updateEmployee(modelMapper.map(e,Employee.class));
        return ResponseEntity.ok(modelMapper.map(employee, EmployeeDto.class));
    }
    @PutMapping("/delete")
    public ResponseEntity<EmployeeDto> deleteEmployee(@RequestParam Long id) {
        Employee employee = employeeService.deleteEmployee(id);
        return ResponseEntity.ok(modelMapper.map(employee, EmployeeDto.class));
    }
    @GetMapping("/get")
    public ResponseEntity<List<EmployeeDto>> getAll() {
        List<Employee> employees = employeeService.getAll();
        List<EmployeeDto> employesDto = new ArrayList<>();
        for (Employee employee : employees) {
            employesDto.add(modelMapper.map(employee, EmployeeDto.class));
        }
        return ResponseEntity.ok(employesDto);
    }
    // Obtener hoja de ruta activa del empleado
    // Obtener TODAS las hojas de ruta del empleado para hoy
    @GetMapping("/{employeeId}/roadmaps")
    public ResponseEntity<?> getEmployeeRoadmaps(@PathVariable Long employeeId) {
        try {
            var roadmaps = employeeService.getAllActiveRoadmaps(employeeId);
            return ResponseEntity.ok(roadmaps);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    // Obtener UNA hoja de ruta específica
    @GetMapping("/{employeeId}/roadmap/{roadmapId}")
    public ResponseEntity<?> getSpecificRoadmap(@PathVariable Long employeeId,
                                                @PathVariable Long roadmapId) {
        try {
            var roadmap = employeeService.getSpecificRoadmap(employeeId, roadmapId);
            return ResponseEntity.ok(roadmap);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    // Procesar QR escaneado
    @PostMapping("/scan-qr")
    public ResponseEntity<?> scanQR(@RequestBody ScanQRRequest request) {
        try {
            var result = employeeService.processScanQR(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<?> getEmployeeByUserId(@PathVariable Long userId) {
        try {
            EmployeesEntity employee = employeeService.getEmployeeByUserId(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("employeeId", employee.getId());
            response.put("name", employee.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    @PostMapping("/{employeeId}/roadmap/close")
    public ResponseEntity<?> closeRoadmap(@PathVariable Long employeeId,
                                          @RequestBody RoadmapCloseRequest request) {
        try {
            RoadmapCloseResponse response = employeeService.closeRoadmap(employeeId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error cerrando hoja de ruta para empleado {}: {}", employeeId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor"));
        }
    }

    /**
     * Obtener rutas completadas listas para cerrar
     */
    @GetMapping("/{employeeId}/roadmaps/completed")
    public ResponseEntity<?> getCompletedRoadmaps(@PathVariable Long employeeId) {
        try {
            // Implementar consulta para rutas COMPLETADAS del empleado
            var completedRoadmaps = employeeService.getCompletedRoadmaps(employeeId);
            return ResponseEntity.ok(completedRoadmaps);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    @GetMapping("/{employeeId}/roadmaps/closeable")
    public ResponseEntity<?> getCloseableRoadmaps(@PathVariable Long employeeId) {
        try {
            var closeableRoadmaps = employeeService.getCloseableRoadmaps(employeeId);
            return ResponseEntity.ok(closeableRoadmaps);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Verificar si una ruta específica puede ser cerrada
     * Útil para mostrar/ocultar el botón de cierre
     */
    @GetMapping("/{employeeId}/roadmap/{roadmapId}/can-close")
    public ResponseEntity<?> canCloseRoadmap(@PathVariable Long employeeId,
                                             @PathVariable Long roadmapId) {
        try {
            boolean canClose = roadmapRepository.canRoadmapBeClosed(employeeId, roadmapId);
            return ResponseEntity.ok(Map.of("canClose", canClose));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}

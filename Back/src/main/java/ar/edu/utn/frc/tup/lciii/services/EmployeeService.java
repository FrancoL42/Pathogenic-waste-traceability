package ar.edu.utn.frc.tup.lciii.services;

import ar.edu.utn.frc.tup.lciii.dtos.EmployeeRoadmapResponse;
import ar.edu.utn.frc.tup.lciii.dtos.RoadmapCloseRequest;
import ar.edu.utn.frc.tup.lciii.dtos.RoadmapCloseResponse;
import ar.edu.utn.frc.tup.lciii.dtos.ScanQRRequest;
import ar.edu.utn.frc.tup.lciii.dtos.ScanQRResponse;
import ar.edu.utn.frc.tup.lciii.entities.EmployeesEntity;
import ar.edu.utn.frc.tup.lciii.models.Employee;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public interface EmployeeService {
    Employee registerEmployee(Employee employee, String email);
    Employee updateEmployee(Employee employee);
    Employee deleteEmployee (Long id);
    List<Employee> getAll();

    List<EmployeeRoadmapResponse> getAllActiveRoadmaps(Long employeeId);

    EmployeeRoadmapResponse getSpecificRoadmap(Long employeeId, Long roadmapId);

    ScanQRResponse processScanQR(ScanQRRequest request);

    @Transactional
    void completeRoadmapManually(Long employeeId, Long roadmapId);

    EmployeesEntity getEmployeeByUserId(Long userId);

    @Transactional
    RoadmapCloseResponse closeRoadmap(Long employeeId, RoadmapCloseRequest request);

    List<EmployeeRoadmapResponse> getCompletedRoadmaps(Long employeeId);

    List<EmployeeRoadmapResponse> getActiveRoadmapsToClose(Long employeeId);

    List<EmployeeRoadmapResponse> getCloseableRoadmaps(Long employeeId);
}

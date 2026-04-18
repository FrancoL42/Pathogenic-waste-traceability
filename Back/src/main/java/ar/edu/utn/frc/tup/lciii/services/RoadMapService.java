package ar.edu.utn.frc.tup.lciii.services;

import ar.edu.utn.frc.tup.lciii.dtos.CreateRoadmapRequest;
import ar.edu.utn.frc.tup.lciii.dtos.RoadmapDto;
import ar.edu.utn.frc.tup.lciii.dtos.WaypointDto;
import ar.edu.utn.frc.tup.lciii.models.Roadmap;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface RoadMapService {
    // Métodos existentes
    Roadmap createRoadMap(RoadmapDto roadMapDto);
    List<WaypointDto> calculateOptimizedRoute(Long roadmapId);

    // NUEVOS MÉTODOS
    RoadmapDto createRoadmapFromOrders(CreateRoadmapRequest request);
    List<RoadmapDto> getAllRoadmaps();
    RoadmapDto getRoadmapById(Long id);
    void checkAndCompleteRoadmap(Long roadmapId);

    // 🆕 MÉTODO MANUAL: Completar ruta manualmente
    @Transactional
    void completeRoadmapManually(Long roadmapId, Long employeeId);
}

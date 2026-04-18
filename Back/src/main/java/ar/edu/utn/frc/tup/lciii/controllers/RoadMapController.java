package ar.edu.utn.frc.tup.lciii.controllers;

import ar.edu.utn.frc.tup.lciii.dtos.CreateRoadmapRequest;
import ar.edu.utn.frc.tup.lciii.dtos.RoadmapDto;
import ar.edu.utn.frc.tup.lciii.dtos.WaypointDto;
import ar.edu.utn.frc.tup.lciii.services.RoadMapService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")

@RestController
@RequestMapping("roadmap")
public class RoadMapController {
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private RoadMapService roadMapService;
    // NUEVO ENDPOINT PARA EL FRONTEND ANGULAR
    @PostMapping("/create-from-orders")
    public ResponseEntity<?> createRoadmapFromOrders(@RequestBody CreateRoadmapRequest request) {
        try {
            RoadmapDto roadmapDto = roadMapService.createRoadmapFromOrders(request);
            return ResponseEntity.ok(roadmapDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor"));
        }
    }

    // Mantener tu endpoint existente
    @PostMapping("/crear")
    public ResponseEntity<RoadmapDto> createRoadMap(@RequestBody RoadmapDto roadmapDto){
        RoadmapDto roadmapDto1 = modelMapper.map(roadMapService.createRoadMap(roadmapDto), RoadmapDto.class);
        return ResponseEntity.ok(roadmapDto1);
    }

    @GetMapping("/{id}/ruta")
    public ResponseEntity<List<WaypointDto>> obtenerRutaOptimizada(@PathVariable Long id) {
        List<WaypointDto> ruta = roadMapService.calculateOptimizedRoute(id);
        return ResponseEntity.ok(ruta);
    }

    // NUEVO: Obtener todas las hojas de ruta
    @GetMapping("/all")
    public ResponseEntity<List<RoadmapDto>> getAllRoadmaps() {
        List<RoadmapDto> roadmaps = roadMapService.getAllRoadmaps();
        return ResponseEntity.ok(roadmaps);
    }
}

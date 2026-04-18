package ar.edu.utn.frc.tup.lciii.controllers;

import ar.edu.utn.frc.tup.lciii.dtos.OptimizedRouteResponse;
import ar.edu.utn.frc.tup.lciii.dtos.RouteOptimizationRequest;
import ar.edu.utn.frc.tup.lciii.services.GeocodingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/routes")
public class RouteOptimizationController {

    @Autowired
    private GeocodingService geocodingService;

    /**
     * Obtener ruta optimizada para una hoja de ruta específica
     */
    @GetMapping("/{roadmapId}/optimize")
    public ResponseEntity<OptimizedRouteResponse> getOptimizedRoute(@PathVariable Long roadmapId) {
        try {
            OptimizedRouteResponse optimizedRoute = geocodingService.optimizarRutaPorId(roadmapId);
            return ResponseEntity.ok(optimizedRoute);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Optimizar ruta con parámetros personalizados
     */
    @PostMapping("/optimize")
    public ResponseEntity<OptimizedRouteResponse> optimizeRouteWithParams(
            @RequestBody RouteOptimizationRequest request) {
        try {
            OptimizedRouteResponse optimizedRoute = geocodingService.optimizarRuta(request);
            return ResponseEntity.ok(optimizedRoute);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Verificar si una ruta puede ser optimizada (tiene coordenadas válidas)
     */
    @GetMapping("/{roadmapId}/can-optimize")
    public ResponseEntity<Map<String, Object>> canOptimizeRoute(@PathVariable Long roadmapId) {
        try {
            // Intentar obtener la ruta optimizada para verificar si es posible
            OptimizedRouteResponse optimizedRoute = geocodingService.optimizarRutaPorId(roadmapId);

            boolean canOptimize = optimizedRoute.getWaypoints() != null &&
                    !optimizedRoute.getWaypoints().isEmpty();

            return ResponseEntity.ok(Map.of(
                    "canOptimize", canOptimize,
                    "totalGenerators", optimizedRoute.getWaypoints().size(),
                    "message", canOptimize ? "Ruta optimizable" : "No hay coordenadas válidas"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "canOptimize", false,
                    "message", "Error al verificar optimización: " + e.getMessage()
            ));
        }
    }

    /**
     * Obtener estadísticas de geocodificación para rutas
     */
    @GetMapping("/geocoding/stats")
    public ResponseEntity<Map<String, Object>> getGeocodingStats() {
        try {
            int sinCoordenadas = geocodingService.contarGeneradoresSinCoordenadas();
            var direccionesSin = geocodingService.obtenerDireccionesSinGeocodificar();

            return ResponseEntity.ok(Map.of(
                    "generatorsWithoutCoordinates", sinCoordenadas,
                    "addressesWithoutCoordinates", direccionesSin.size(),
                    "sampleAddresses", direccionesSin.stream().limit(5).toList()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al obtener estadísticas: " + e.getMessage()
            ));
        }
    }

    /**
     * Validar si una dirección puede ser geocodificada
     */
    @GetMapping("/validate-address")
    public ResponseEntity<Map<String, Object>> validateAddress(@RequestParam String address) {
        try {
            boolean isValid = geocodingService.validarDireccion(address);
            return ResponseEntity.ok(Map.of(
                    "isValid", isValid,
                    "address", address,
                    "message", isValid ? "Dirección válida" : "Dirección no geocodificable"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "isValid", false,
                    "message", "Error validando dirección: " + e.getMessage()
            ));
        }
    }

    /**
     * Geocodificar una dirección específica
     */
    @GetMapping("/geocode")
    public ResponseEntity<Map<String, Object>> geocodeAddress(@RequestParam String address) {
        try {
            var coordenadas = geocodingService.obtenerCoordenadasDesdeDireccion(address);

            if (coordenadas.isPresent()) {
                double[] coords = coordenadas.get();
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "latitude", coords[0],
                        "longitude", coords[1],
                        "address", address
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "No se pudo geocodificar la dirección",
                        "address", address
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Error geocodificando: " + e.getMessage()
            ));
        }
    }
}
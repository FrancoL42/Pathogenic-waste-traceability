package ar.edu.utn.frc.tup.lciii.controllers;

import ar.edu.utn.frc.tup.lciii.clients.GeoMapsClient;
import ar.edu.utn.frc.tup.lciii.dtos.GeocodingResponse;
import ar.edu.utn.frc.tup.lciii.services.GeocodingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/admin/geocoding")
public class GeocodingAdminController {

    @Autowired
    private GeocodingService geocodingService;
    @Autowired
    private GeoMapsClient geoMapsClient;

    @GetMapping("/test-geocoding")
    public ResponseEntity<?> testGeocoding() {
        try {
            Optional<double[]> resultado = geoMapsClient.geocode("Avenida Emilio Olmos 237, Córdoba, Argentina")
                    .map(respuesta -> new double[]{
                            Double.parseDouble(respuesta.getLat()),
                            Double.parseDouble(respuesta.getLon())
                    });

            if (resultado.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "coordinates", resultado.get()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "No se encontraron coordenadas"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage(),
                    "stackTrace", Arrays.toString(e.getStackTrace())
            ));
        }
    }

    /**
     * ENDPOINT PARA ADMIN: Inicializar geocodificación masiva
     * Usar este endpoint una vez para geocodificar todos los generadores existentes
     */
    @PostMapping("/initialize")
    public ResponseEntity<?> initializeGeocoding() {
        try {
            int sinCoordenadas = geocodingService.contarGeneradoresSinCoordenadas();

            if (sinCoordenadas == 0) {
                return ResponseEntity.ok(Map.of(
                        "message", "✅ Todos los generadores ya tienen coordenadas",
                        "total", 0,
                        "status", "COMPLETED"
                ));
            }

            // Ejecutar en hilo separado para no bloquear
            new Thread(() -> {
                System.out.println("🚀 Iniciando geocodificación masiva de " + sinCoordenadas + " generadores...");
                geocodingService.geocodificarGeneradores();
                System.out.println("🏁 Geocodificación masiva completada");
            }).start();

            return ResponseEntity.ok(Map.of(
                    "message", "🚀 Iniciada geocodificación de " + sinCoordenadas + " generadores",
                    "total", sinCoordenadas,
                    "status", "PROCESSING",
                    "estimatedTimeMinutes", sinCoordenadas * 2 // 2 minutos por generador aprox
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al inicializar geocodificación: " + e.getMessage()
            ));
        }
    }
    @GetMapping("/test")
    public ResponseEntity<String> testGeocodingSpecific() {
        System.out.println("\n=== PRUEBA DE GEOCODIFICACIÓN ===");

        // Probar con direcciones conocidas de Córdoba
        String[] testAddresses = {
                "San Martín 1000, Córdoba, Argentina",
                "Belgrano 500, Córdoba, Argentina",
                "Colón 300, Córdoba, Argentina",
                "Roma 351, Córdoba, Argentina", // Tu dirección problemática
                "Dean Funes 200, Córdoba, Argentina"
        };

        StringBuilder results = new StringBuilder();

        for (String address : testAddresses) {
            results.append("\n--- TESTING: ").append(address).append(" ---\n");

            Optional<GeocodingResponse> result = geoMapsClient.geocode(address);
            if (result.isPresent()) {
                GeocodingResponse resp = result.get();
                results.append("✅ ENCONTRADA\n");
                results.append("📍 Coords: ").append(resp.getLat()).append(", ").append(resp.getLon()).append("\n");
                results.append("🏠 ").append(resp.getDisplayName()).append("\n");
            } else {
                results.append("❌ NO ENCONTRADA\n");
            }
            results.append("\n");
        }

        return ResponseEntity.ok(results.toString());
    }
    /**
     * Verificar progreso de geocodificación
     */
    @GetMapping("/status")
    public ResponseEntity<?> getGeocodingStatus() {
        try {
            int sinCoordenadas = geocodingService.contarGeneradoresSinCoordenadas();
            var direccionesSin = geocodingService.obtenerDireccionesSinGeocodificar();

            return ResponseEntity.ok(Map.of(
                    "generatorsWithoutCoordinates", sinCoordenadas,
                    "isComplete", sinCoordenadas == 0,
                    "sampleMissingAddresses", direccionesSin.stream().limit(3).toList(),
                    "totalMissingAddresses", direccionesSin.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al obtener estado: " + e.getMessage()
            ));
        }
    }

    /**
     * Manual: geocodificar generadores específicos por ID
     */
    @PostMapping("/geocode-specific")
    public ResponseEntity<?> geocodeSpecificGenerators(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            var generatorIds = (java.util.List<Integer>) request.get("generatorIds");

            if (generatorIds == null || generatorIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Debe proporcionar una lista de IDs de generadores"
                ));
            }

            // Procesar en hilo separado
            new Thread(() -> {
                for (Integer id : generatorIds) {
                    try {
                        geocodingService.geocodificarGeneradorPorId(id.longValue());
                        Thread.sleep(1500); // Pausa entre requests
                    } catch (Exception e) {
                        System.err.println("Error geocodificando generador " + id + ": " + e.getMessage());
                    }
                }
            }).start();

            return ResponseEntity.ok(Map.of(
                    "message", "Iniciada geocodificación de " + generatorIds.size() + " generadores específicos",
                    "generatorIds", generatorIds,
                    "status", "PROCESSING"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error en geocodificación específica: " + e.getMessage()
            ));
        }
    }
}
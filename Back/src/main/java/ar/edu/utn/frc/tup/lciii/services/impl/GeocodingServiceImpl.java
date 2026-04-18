package ar.edu.utn.frc.tup.lciii.services.impl;

import ar.edu.utn.frc.tup.lciii.clients.GeoMapsClient;
import ar.edu.utn.frc.tup.lciii.dtos.*;
import ar.edu.utn.frc.tup.lciii.entities.GeneratorEntity;
import ar.edu.utn.frc.tup.lciii.entities.RoadmapDetailEntity;
import ar.edu.utn.frc.tup.lciii.entities.RoadmapEntity;
import ar.edu.utn.frc.tup.lciii.repositories.GeneratorRepository;
import ar.edu.utn.frc.tup.lciii.repositories.RoadmapRepository;
import ar.edu.utn.frc.tup.lciii.services.GeocodingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class GeocodingServiceImpl implements GeocodingService {

    @Autowired
    private GeoMapsClient geoMapsClient;

    @Autowired
    private GeneratorRepository generatorRepository;

    @Autowired
    private RoadmapRepository roadmapRepository;

    @Value("${geolocation.depot.latitude:-31.4135}")
    private double depotLatitude;

    @Value("${geolocation.depot.longitude:-64.1810}")
    private double depotLongitude;

    @Value("${route.optimization.vehicle-speed:40.0}")
    private double vehicleSpeed;


    // GEOCODING BÁSICO
    @Override
    public Optional<double[]> obtenerCoordenadasDesdeDireccion(String direccion) {
        return geoMapsClient.geocode(direccion)
                .map(respuesta -> new double[]{
                        Double.parseDouble(respuesta.getLat()),
                        Double.parseDouble(respuesta.getLon())
                });
    }

    @Override
    public Optional<String> obtenerDireccionDesdeCoordenas(double latitude, double longitude) {
        return Optional.of("");
    }

    // CÁLCULOS DE DISTANCIA Y TIEMPO
    @Override
    public double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        return 0;
    }

    @Override
    public int calcularTiempoEstimado(double distanciaKm, double velocidadPromedio) {
        return 0;
    }

    // GEOCODING EN LOTE

    @Override
    @Transactional
    public void geocodificarGeneradores() {
        List<GeneratorEntity> generadoresSinCoordenadas = generatorRepository
                .findByLatitudeIsNullOrLongitudeIsNull();

        System.out.println("📍 Iniciando geocodificación de " +
                generadoresSinCoordenadas.size() + " generadores");

        int geocodificados = 0;
        int errores = 0;

        for (GeneratorEntity generador : generadoresSinCoordenadas) {
            try {
                if (esDirectcionValida(generador.getAddress())) {
                if (generador.getAddress() != null && !generador.getAddress().trim().isEmpty()) {
                    Optional<double[]> coordenadas = obtenerCoordenadasDesdeDireccion(
                            generador.getAddress() + ", Córdoba, Argentina");

                    if (coordenadas.isPresent()) {
                        double[] coords = coordenadas.get();
                        generador.setLatitude(coords[0]);
                        generador.setLongitude(coords[1]);
                        generatorRepository.save(generador);
                        geocodificados++;

                        System.out.println("✅ Geocodificado: " + generador.getName() +
                                " -> (" + coords[0] + ", " + coords[1] + ")");
                    } else {
                        errores++;
                        System.out.println("❌ No se pudo geocodificar: " +
                                generador.getName() + " - " + generador.getAddress());
                    }
                } else {
                    errores++;
                    System.out.println("⚠️ Sin dirección: " + generador.getName());
                }} else {
                    errores++;
                    System.out.println("⚠️ Dirección inválida omitida: " + generador.getName() + " - " + generador.getAddress());
                }


                Thread.sleep(200);
            } catch (Exception e) {
                errores++;
                System.err.println("💥 Error geocodificando " + generador.getName() + ": " + e.getMessage());
            }
        }

        System.out.println("🏁 Geocodificación completada: " + geocodificados +
                " exitosos, " + errores + " errores");
    }

    @Override
    @Transactional
    public void geocodificarGeneradorPorId(Long generadorId) {
        Optional<GeneratorEntity> generadorOpt = generatorRepository.findById(generadorId);

        if (generadorOpt.isPresent()) {
            GeneratorEntity generador = generadorOpt.get();

            if (generador.getAddress() != null && !generador.getAddress().trim().isEmpty()) {
                Optional<double[]> coordenadas = obtenerCoordenadasDesdeDireccion(
                        generador.getAddress() + ", Córdoba, Argentina");

                if (coordenadas.isPresent()) {
                    double[] coords = coordenadas.get();
                    generador.setLatitude(coords[0]);
                    generador.setLongitude(coords[1]);
                    generatorRepository.save(generador);

                    System.out.println("✅ Generador geocodificado: " + generador.getName());
                } else {
                    System.out.println("❌ No se pudo geocodificar: " + generador.getAddress());
                }
            }
        }
    }
    private boolean esDirectcionValida(String direccion) {
        if (direccion == null || direccion.trim().length() < 5) return false;

        String dir = direccion.toLowerCase();
        return !dir.contains("calle x") && !dir.contains("qqweqwe") && !dir.contains("qwdqwd");
    }
    @Override
    public boolean validarDireccion(String direccion) {
        return true;
    }


    // OPTIMIZACIÓN DE RUTAS
    @Override
    public OptimizedRouteResponse optimizarRuta(RouteOptimizationRequest request) {
        Optional<RoadmapEntity> roadmapOpt = roadmapRepository.findById(request.getRoadmapId());

        if (roadmapOpt.isEmpty()) {
            throw new IllegalArgumentException("Hoja de ruta no encontrada");
        }

        RoadmapEntity roadmap = roadmapOpt.get();
        return optimizarRutaInterno(roadmap, request);
    }

    @Override
    public OptimizedRouteResponse optimizarRutaPorId(Long roadmapId) {
        Optional<RoadmapEntity> roadmapOpt = roadmapRepository.findById(roadmapId);

        if (roadmapOpt.isEmpty()) {
            throw new IllegalArgumentException("Hoja de ruta no encontrada");
        }

        RoadmapEntity roadmap = roadmapOpt.get();

        // Crear request por defecto
        RouteOptimizationRequest request = new RouteOptimizationRequest();
        request.setRoadmapId(roadmapId);
        request.setStartLatitude(depotLatitude);
        request.setStartLongitude(depotLongitude);
        request.setStartTime(roadmap.getExitHour() != null ?
                roadmap.getExitHour().toString() : "08:00");
        request.setIncludeReturnToDepot(true);
        request.setOptimizationMethod("NEAREST_NEIGHBOR");

        return optimizarRutaInterno(roadmap, request);
    }


    // OPTIMIZACIÓN INTERNA
    private OptimizedRouteResponse optimizarRutaInterno(RoadmapEntity roadmap, RouteOptimizationRequest request) {
        // 1. Obtener generadores con coordenadas válidas
        List<GeneratorEntity> generadores = roadmap.getDetails().stream()
                .map(RoadmapDetailEntity::getGeneratorEntity)
                .filter(g -> g.getLatitude() != null && g.getLongitude() != null)
                .collect(Collectors.toList());

        if (generadores.isEmpty()) {
            throw new IllegalArgumentException("No hay generadores con coordenadas válidas en esta ruta");
        }

        // 2. Implementar algoritmo Nearest Neighbor
        List<RouteWaypoint> waypoints = calcularRutaOptimizada(
                generadores,
                request.getStartLatitude(),
                request.getStartLongitude(),
                request.getStartTime()
        );

        // 3. Calcular resumen
        RouteSummary summary = calcularResumenRuta(waypoints, request.getStartTime());

        // 4. Crear respuesta
        OptimizedRouteResponse response = new OptimizedRouteResponse();
        response.setRoadmapId(roadmap.getRoadmapId());
        response.setZone(roadmap.getZone());
        response.setWaypoints(waypoints);
        response.setSummary(summary);
        response.setStatus("OPTIMIZADA");

        return response;
    }

    // ALGORITMO NEAREST NEIGHBOR
    private List<RouteWaypoint> calcularRutaOptimizada(List<GeneratorEntity> generadores,
                                                       double startLat, double startLon, String startTime) {
        List<RouteWaypoint> waypoints = new ArrayList<>();
        List<GeneratorEntity> noVisitados = new ArrayList<>(generadores);

        double currentLat = startLat;
        double currentLon = startLon;
        LocalTime currentTime = LocalTime.parse(startTime);
        int order = 1;

        while (!noVisitados.isEmpty()) {
            // Encontrar el generador más cercano
            GeneratorEntity masRecano = encontrarMasCercano(currentLat, currentLon, noVisitados);
            noVisitados.remove(masRecano);

            // Calcular distancia y tiempo
            double distancia = calcularDistancia(currentLat, currentLon,
                    masRecano.getLatitude(), masRecano.getLongitude());
            int tiempoViaje = calcularTiempoEstimado(distancia, vehicleSpeed);
            int tiempoRecoleccion = 15; // 15 minutos por generador por defecto

            // Calcular hora de llegada
            currentTime = currentTime.plusMinutes(tiempoViaje);

            // Crear waypoint
            RouteWaypoint waypoint = new RouteWaypoint();
            waypoint.setOrder(order++);
            waypoint.setGeneratorId(masRecano.getGeneratorId());
            waypoint.setGeneratorName(masRecano.getName());
            waypoint.setAddress(masRecano.getAddress());
            waypoint.setLatitude(masRecano.getLatitude());
            waypoint.setLongitude(masRecano.getLongitude());
            waypoint.setEstimatedBags(5); // Por defecto, debería venir de los precintos
            waypoint.setEstimatedTimeMinutes(tiempoViaje + tiempoRecoleccion);
            waypoint.setDistanceFromPrevious(distancia);
            waypoint.setArrivalTime(currentTime.format(DateTimeFormatter.ofPattern("HH:mm")));
            waypoint.setStatus("PENDING");

            waypoints.add(waypoint);

            // Actualizar posición actual
            currentLat = masRecano.getLatitude();
            currentLon = masRecano.getLongitude();
            currentTime = currentTime.plusMinutes(tiempoRecoleccion);
        }

        return waypoints;
    }

    private GeneratorEntity encontrarMasCercano(double lat, double lon, List<GeneratorEntity> candidatos) {
        return candidatos.stream()
                .min(Comparator.comparingDouble(g ->
                        calcularDistancia(lat, lon, g.getLatitude(), g.getLongitude())))
                .orElseThrow(() -> new IllegalStateException("No se pudo encontrar el generador más cercano"));
    }


    // CÁLCULO DE RESUMEN

    private RouteSummary calcularResumenRuta(List<RouteWaypoint> waypoints, String startTime) {
        RouteSummary summary = new RouteSummary();

        double totalDistance = waypoints.stream()
                .mapToDouble(RouteWaypoint::getDistanceFromPrevious)
                .sum();

        int totalTime = waypoints.stream()
                .mapToInt(RouteWaypoint::getEstimatedTimeMinutes)
                .sum();

        int totalBags = waypoints.stream()
                .mapToInt(RouteWaypoint::getEstimatedBags)
                .sum();

        LocalTime endTime = LocalTime.parse(startTime).plusMinutes(totalTime);

        summary.setTotalDistance(Math.round(totalDistance * 100.0) / 100.0);
        summary.setTotalTimeMinutes(totalTime);
        summary.setTotalGenerators(waypoints.size());
        summary.setTotalBags(totalBags);
        summary.setStartTime(startTime);
        summary.setEstimatedEndTime(endTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        summary.setOptimizationMethod("NEAREST_NEIGHBOR");

        return summary;
    }

    // UTILIDADES

    @Override
    public List<String> obtenerDireccionesSinGeocodificar() {
        return generatorRepository.findByLatitudeIsNullOrLongitudeIsNull()
                .stream()
                .map(GeneratorEntity::getAddress)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public int contarGeneradoresSinCoordenadas() {
        return generatorRepository.findByLatitudeIsNullOrLongitudeIsNull().size();
    }
}
package ar.edu.utn.frc.tup.lciii.services;

import ar.edu.utn.frc.tup.lciii.dtos.GeocodingResponse;
import ar.edu.utn.frc.tup.lciii.dtos.OptimizedRouteResponse;
import ar.edu.utn.frc.tup.lciii.dtos.RouteOptimizationRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface GeocodingService {

    // ===================================================
    // GEOCODING BÁSICO
    // ===================================================
    Optional<double[]> obtenerCoordenadasDesdeDireccion(String direccion);
    Optional<String> obtenerDireccionDesdeCoordenas(double latitude, double longitude);

    // ===================================================
    // CÁLCULOS DE DISTANCIA Y TIEMPO
    // ===================================================
    double calcularDistancia(double lat1, double lon1, double lat2, double lon2);
    int calcularTiempoEstimado(double distanciaKm, double velocidadPromedio);

    // ===================================================
    // GEOCODING EN LOTE
    // ===================================================
    void geocodificarGeneradores();
    void geocodificarGeneradorPorId(Long generadorId);
    boolean validarDireccion(String direccion);

    // ===================================================
    // OPTIMIZACIÓN DE RUTAS
    // ===================================================
    OptimizedRouteResponse optimizarRuta(RouteOptimizationRequest request);
    OptimizedRouteResponse optimizarRutaPorId(Long roadmapId);

    // ===================================================
    // UTILIDADES
    // ===================================================
    List<String> obtenerDireccionesSinGeocodificar();
    int contarGeneradoresSinCoordenadas();
}
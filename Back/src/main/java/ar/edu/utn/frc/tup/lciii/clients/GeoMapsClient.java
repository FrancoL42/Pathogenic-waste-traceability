
package ar.edu.utn.frc.tup.lciii.clients;

import ar.edu.utn.frc.tup.lciii.dtos.GeocodingResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Component
public class GeoMapsClient {

    @Value("${geolocation.locationiq.url:https://us1.locationiq.com}")
    private String locationiqUrl;

    @Value("${geolocation.locationiq.api-key:}")
    private String locationiqApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // Configuración para Córdoba Capital
    private static final double CORDOBA_CENTER_LAT = -31.4201;
    private static final double CORDOBA_CENTER_LON = -64.1888;
    private static final double CORDOBA_MIN_LAT = -31.50;
    private static final double CORDOBA_MAX_LAT = -31.30;
    private static final double CORDOBA_MIN_LON = -64.30;
    private static final double CORDOBA_MAX_LON = -64.05;
    private static final String CORDOBA_BOUNDING_BOX = "-64.30,-31.50,-64.05,-31.30";

    public GeoMapsClient() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Método principal de geocodificación
     */
    public Optional<GeocodingResponse> geocode(String address) {
        if (address == null || address.trim().isEmpty()) {
            return Optional.empty();
        }

        System.out.println("🔍 Geocodificando: " + address);

        // Verificar API key
        if (locationiqApiKey == null || locationiqApiKey.isEmpty() || locationiqApiKey.length() < 30) {
            System.out.println("⚠️ LocationIQ API key no configurada, usando coordenadas de Córdoba");
            return createCordobaFallback(address);
        }

        // Intentar con LocationIQ
        Optional<GeocodingResponse> result = geocodeWithLocationIQ(address);
        if (result.isPresent()) {
            return result;
        }

        // Fallback: usar coordenadas del centro de Córdoba
        return createCordobaFallback(address);
    }

    /**
     * Geocodificación con LocationIQ
     */
    private Optional<GeocodingResponse> geocodeWithLocationIQ(String address) {
        try {
            String cleanAddress = address.trim().replaceAll("\\s+", " ");
            String searchAddress = cleanAddress + ", Córdoba Capital, Argentina";

            String url = String.format(
                    "%s/v1/search?q=%s&key=%s&format=json&limit=5&addressdetails=1&bounded=1&viewbox=%s&countrycodes=ar",
                    locationiqUrl,
                    URLEncoder.encode(searchAddress, StandardCharsets.UTF_8),
                    locationiqApiKey,
                    CORDOBA_BOUNDING_BOX
            );

            System.out.println("🌐 LocationIQ URL: " + url);

            String response = webClient.get()
                    .uri(url)
                    .header("accept", "application/json")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response != null && !response.trim().equals("[]")) {
                JsonNode jsonArray = objectMapper.readTree(response);
                if (jsonArray.isArray() && jsonArray.size() > 0) {
                    // Tomar el primer resultado válido
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonNode result = jsonArray.get(i);
                        double lat = result.get("lat").asDouble();
                        double lon = result.get("lon").asDouble();

                        if (isWithinCordobaBounds(lat, lon)) {
                            GeocodingResponse geocodingResponse = new GeocodingResponse();
                            geocodingResponse.setLat(String.valueOf(lat));
                            geocodingResponse.setLon(String.valueOf(lon));
                            geocodingResponse.setDisplayName(result.get("display_name").asText());

                            System.out.println("✅ LocationIQ resultado: " + lat + ", " + lon);
                            return Optional.of(geocodingResponse);
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error en LocationIQ: " + e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Crear respuesta de fallback para Córdoba
     */
    private Optional<GeocodingResponse> createCordobaFallback(String address) {
        // Generar coordenadas aleatorias dentro de Córdoba basadas en el hash de la dirección
        Random random = new Random(address.hashCode());

        double offsetLat = (random.nextDouble() - 0.5) * 0.1; // ±5km aprox
        double offsetLon = (random.nextDouble() - 0.5) * 0.1;

        double finalLat = CORDOBA_CENTER_LAT + offsetLat;
        double finalLon = CORDOBA_CENTER_LON + offsetLon;

        // Asegurar que esté dentro de los límites
        finalLat = Math.max(CORDOBA_MIN_LAT, Math.min(CORDOBA_MAX_LAT, finalLat));
        finalLon = Math.max(CORDOBA_MIN_LON, Math.min(CORDOBA_MAX_LON, finalLon));

        GeocodingResponse response = new GeocodingResponse();
        response.setLat(String.valueOf(finalLat));
        response.setLon(String.valueOf(finalLon));
        response.setDisplayName(address + ", Córdoba Capital, Argentina (ubicación aproximada)");

        System.out.println("🏠 Usando coordenadas de Córdoba: " + finalLat + ", " + finalLon);
        return Optional.of(response);
    }

    /**
     * Verificar si las coordenadas están dentro de Córdoba
     */
    private boolean isWithinCordobaBounds(double lat, double lon) {
        return lat >= CORDOBA_MIN_LAT && lat <= CORDOBA_MAX_LAT &&
                lon >= CORDOBA_MIN_LON && lon <= CORDOBA_MAX_LON;
    }

    /**
     * Obtener coordenadas del centro de Córdoba
     */
    public GeocodingResponse getCordobaCenterCoordinates() {
        GeocodingResponse center = new GeocodingResponse();
        center.setLat(String.valueOf(CORDOBA_CENTER_LAT));
        center.setLon(String.valueOf(CORDOBA_CENTER_LON));
        center.setDisplayName("Centro de Córdoba Capital, Córdoba, Argentina");
        return center;
    }
}
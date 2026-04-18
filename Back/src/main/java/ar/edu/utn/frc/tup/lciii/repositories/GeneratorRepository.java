package ar.edu.utn.frc.tup.lciii.repositories;

import ar.edu.utn.frc.tup.lciii.entities.GeneratorEntity;
import ar.edu.utn.frc.tup.lciii.models.RegisterState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GeneratorRepository extends JpaRepository<GeneratorEntity,Long> {
    GeneratorEntity findByUserId(Long id);
    GeneratorEntity getGeneratorEntityByName(String name);
    List<GeneratorEntity> getGeneratorEntityByState(RegisterState state);

    // ===================================================
    // NUEVAS CONSULTAS PARA GEOLOCALIZACIÓN
    // ===================================================

    // Generadores sin coordenadas (para geocodificar)
    @Query("SELECT g FROM GeneratorEntity g WHERE g.latitude IS NULL OR g.longitude IS NULL")
    List<GeneratorEntity> findByLatitudeIsNullOrLongitudeIsNull();

    // Generadores con coordenadas válidas
    @Query("SELECT g FROM GeneratorEntity g WHERE g.latitude IS NOT NULL AND g.longitude IS NOT NULL")
    List<GeneratorEntity> findByLatitudeIsNotNullAndLongitudeIsNotNull();

    // Generadores en un radio específico (en kilómetros)
    @Query(value = """
        SELECT * FROM Generadores g 
        WHERE g.latitud IS NOT NULL AND g.longitud IS NOT NULL
        AND (6371 * acos(cos(radians(?1)) * cos(radians(g.latitud)) * 
             cos(radians(g.longitud) - radians(?2)) + sin(radians(?1)) * 
             sin(radians(g.latitud)))) <= ?3
        """, nativeQuery = true)
    List<GeneratorEntity> findGeneratorsWithinRadius(double latitude, double longitude, double radiusKm);

    // Generadores por zona con coordenadas
    @Query("""
        SELECT g FROM GeneratorEntity g 
        JOIN OrdersEntity o ON o.generatorEntity = g 
        JOIN ZoneEntity z ON o.zoneEntity = z 
        WHERE z.name = ?1 AND g.latitude IS NOT NULL AND g.longitude IS NOT NULL
        """)
    List<GeneratorEntity> findByZoneWithCoordinates(String zoneName);

    // Contar generadores sin coordenadas por estado
    @Query("SELECT COUNT(g) FROM GeneratorEntity g WHERE (g.latitude IS NULL OR g.longitude IS NULL) AND g.state = ?1")
    long countByStateAndMissingCoordinates(RegisterState state);

    Optional<GeneratorEntity> findByEmail(String email);
}
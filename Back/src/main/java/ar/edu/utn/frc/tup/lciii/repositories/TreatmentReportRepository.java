package ar.edu.utn.frc.tup.lciii.repositories;

import ar.edu.utn.frc.tup.lciii.entities.SealEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TreatmentReportRepository extends JpaRepository<SealEntity, Long> {
    /**
     * Obtiene los datos de tratamientos agrupados por fecha, generador y zona
     */
    @Query("""
        SELECT 
            s.fechaTratamiento,
            g.name,
            g.type,
            z.name,
            COUNT(s.sealId),
            COALESCE(SUM(s.peso), 0.0)
        FROM SealEntity s
        INNER JOIN s.generatorEntity g
        LEFT JOIN g.zone z
        WHERE s.state = 'TRATADO' 
            AND s.fechaTratamiento IS NOT NULL
            AND (:fechaInicio IS NULL OR s.fechaTratamiento >= :fechaInicio)
            AND (:fechaFin IS NULL OR s.fechaTratamiento <= :fechaFin)
            AND (:tipoGenerador IS NULL OR g.type = :tipoGenerador)
            AND (:zona IS NULL OR z.name = :zona)
        GROUP BY s.fechaTratamiento, g.generatorId, g.name, g.type, z.name
        ORDER BY s.fechaTratamiento DESC, g.name
    """)
    List<Object[]> getTreatmentData(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("tipoGenerador") String tipoGenerador,
            @Param("zona") String zona
    );

    /**
     * Obtiene las zonas disponibles donde se han realizado tratamientos
     */
    @Query("""
        SELECT DISTINCT z.name
        FROM SealEntity s
        INNER JOIN s.generatorEntity g
        LEFT JOIN g.zone z
        WHERE s.state = 'TRATADO' 
            AND s.fechaTratamiento IS NOT NULL
            AND z.name IS NOT NULL
        ORDER BY z.name
    """)
    List<String> getAvailableZones();

    /**
     * Obtiene estadísticas diarias de tratamientos para gráficos
     */
    @Query("""
        SELECT 
            s.fechaTratamiento,
            COUNT(s.sealId),
            COALESCE(SUM(s.peso), 0.0)
        FROM SealEntity s
        INNER JOIN s.generatorEntity g
        LEFT JOIN g.zone z
        WHERE s.state = 'TRATADO' 
            AND s.fechaTratamiento IS NOT NULL
            AND (:fechaInicio IS NULL OR s.fechaTratamiento >= :fechaInicio)
            AND (:fechaFin IS NULL OR s.fechaTratamiento <= :fechaFin)
            AND (:tipoGenerador IS NULL OR g.type = :tipoGenerador)
            AND (:zona IS NULL OR z.name = :zona)
        GROUP BY s.fechaTratamiento
        ORDER BY s.fechaTratamiento
    """)
    List<Object[]> getDailyTreatmentStats(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("tipoGenerador") String tipoGenerador,
            @Param("zona") String zona
    );

    /**
     * Obtiene estadísticas por zona para gráficos circulares
     */
    @Query("""
        SELECT 
            COALESCE(z.name, 'Sin zona'),
            COUNT(s.sealId),
            COALESCE(SUM(s.peso), 0.0)
        FROM SealEntity s
        INNER JOIN s.generatorEntity g
        LEFT JOIN g.zone z
        WHERE s.state = 'TRATADO' 
            AND s.fechaTratamiento IS NOT NULL
            AND (:fechaInicio IS NULL OR s.fechaTratamiento >= :fechaInicio)
            AND (:fechaFin IS NULL OR s.fechaTratamiento <= :fechaFin)
            AND (:tipoGenerador IS NULL OR g.type = :tipoGenerador)
            AND (:zona IS NULL OR z.name = :zona)
        GROUP BY z.name
        ORDER BY COUNT(s.sealId) DESC
    """)
    List<Object[]> getTreatmentStatsByZone(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("tipoGenerador") String tipoGenerador,
            @Param("zona") String zona
    );

    /**
     * Obtiene estadísticas por tipo de generador
     */
    @Query("""
        SELECT 
            g.type,
            COUNT(s.sealId),
            COALESCE(SUM(s.peso), 0.0)
        FROM SealEntity s
        INNER JOIN s.generatorEntity g
        LEFT JOIN g.zone z
        WHERE s.state = 'TRATADO' 
            AND s.fechaTratamiento IS NOT NULL
            AND (:fechaInicio IS NULL OR s.fechaTratamiento >= :fechaInicio)
            AND (:fechaFin IS NULL OR s.fechaTratamiento <= :fechaFin)
            AND (:tipoGenerador IS NULL OR g.type = :tipoGenerador)
            AND (:zona IS NULL OR z.name = :zona)
        GROUP BY g.type
        ORDER BY COUNT(s.sealId) DESC
    """)
    List<Object[]> getTreatmentStatsByType(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("tipoGenerador") String tipoGenerador,
            @Param("zona") String zona
    );

    /**
     * Obtiene el top 10 de generadores con más tratamientos
     */
    @Query("""
        SELECT 
            g.name,
            COUNT(s.sealId),
            COALESCE(SUM(s.peso), 0.0)
        FROM SealEntity s
        INNER JOIN s.generatorEntity g
        LEFT JOIN g.zone z
        WHERE s.state = 'TRATADO' 
            AND s.fechaTratamiento IS NOT NULL
            AND (:fechaInicio IS NULL OR s.fechaTratamiento >= :fechaInicio)
            AND (:fechaFin IS NULL OR s.fechaTratamiento <= :fechaFin)
            AND (:tipoGenerador IS NULL OR g.type = :tipoGenerador)
            AND (:zona IS NULL OR z.name = :zona)
        GROUP BY g.generatorId, g.name
        ORDER BY COUNT(s.sealId) DESC
        LIMIT 10
    """)
    List<Object[]> getTopGeneratorsByTreatments(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("tipoGenerador") String tipoGenerador,
            @Param("zona") String zona
    );
}

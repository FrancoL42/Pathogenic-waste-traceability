package ar.edu.utn.frc.tup.lciii.repositories;

import ar.edu.utn.frc.tup.lciii.entities.SalesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SalesReportRepository extends JpaRepository<SalesEntity, Long> {

    /**
     * Obtiene datos de ventas para el reporte con filtros opcionales
     */
    @Query("""
        SELECT s FROM SalesEntity s 
        JOIN s.generatorEntity g 
        LEFT JOIN g.zone z 
        WHERE (:fechaInicio IS NULL OR s.date >= :fechaInicio) 
        AND (:fechaFin IS NULL OR s.date <= :fechaFin) 
        AND (:tipoGenerador IS NULL OR g.type = :tipoGenerador) 
        AND (:zona IS NULL OR z.name = :zona)
        ORDER BY s.date DESC
        """)
    List<SalesEntity> findSalesForReport(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            @Param("tipoGenerador") String tipoGenerador,
            @Param("zona") String zona
    );

    /**
     * Obtiene el total de ventas por generador en un período
     */
    @Query("""
        SELECT g.name, COUNT(s.saleId), COALESCE(SUM(s.quantity), 0), COALESCE(SUM(s.totalBuy), 0), MAX(s.date)
        FROM SalesEntity s 
        JOIN s.generatorEntity g 
        LEFT JOIN g.zone z 
        WHERE (:fechaInicio IS NULL OR s.date >= :fechaInicio) 
        AND (:fechaFin IS NULL OR s.date <= :fechaFin) 
        AND (:tipoGenerador IS NULL OR g.type = :tipoGenerador) 
        AND (:zona IS NULL OR z.name = :zona)
        AND g.name IS NOT NULL
        GROUP BY g.generatorId, g.name, g.type, z.name
        ORDER BY COALESCE(SUM(s.quantity), 0) DESC
        """)
    List<Object[]> getSalesAggregatedByGenerator(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            @Param("tipoGenerador") String tipoGenerador,
            @Param("zona") String zona
    );

    /**
     * Obtiene estadísticas por tipo de generador
     */
    @Query("""
        SELECT g.type, COUNT(DISTINCT g.generatorId), COALESCE(SUM(s.quantity), 0), COALESCE(SUM(s.totalBuy), 0)
        FROM SalesEntity s 
        JOIN s.generatorEntity g 
        LEFT JOIN g.zone z 
        WHERE (:fechaInicio IS NULL OR s.date >= :fechaInicio) 
        AND (:fechaFin IS NULL OR s.date <= :fechaFin) 
        AND (:tipoGenerador IS NULL OR g.type = :tipoGenerador) 
        AND (:zona IS NULL OR z.name = :zona)
        AND g.type IS NOT NULL
        GROUP BY g.type
        """)
    List<Object[]> getSalesStatsByType(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            @Param("tipoGenerador") String tipoGenerador,
            @Param("zona") String zona
    );

    /**
     * Obtiene todas las zonas disponibles que tienen generadores
     */
    @Query("""
        SELECT DISTINCT z.name 
        FROM ZoneEntity z 
        WHERE z.name IS NOT NULL 
        ORDER BY z.name
        """)
    List<String> getAvailableZones();

    /**
     * Obtiene datos para calcular crecimiento mensual por generador
     */
    @Query("""
        SELECT g.name, 
               YEAR(s.date), 
               MONTH(s.date), 
               SUM(s.quantity)
        FROM SalesEntity s 
        JOIN s.generatorEntity g 
        WHERE s.date >= :fechaInicio 
        AND s.date <= :fechaFin
        GROUP BY g.generatorId, g.name, YEAR(s.date), MONTH(s.date)
        ORDER BY g.name, YEAR(s.date), MONTH(s.date)
        """)
    List<Object[]> getMonthlyDataForGrowthCalculation(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );

    /**
     * Obtiene el generador con más ventas en cantidad de bolsas
     */
    @Query("""
        SELECT g.name
        FROM SalesEntity s 
        JOIN s.generatorEntity g 
        LEFT JOIN g.zone z 
        WHERE (:fechaInicio IS NULL OR s.date >= :fechaInicio) 
        AND (:fechaFin IS NULL OR s.date <= :fechaFin) 
        AND (:tipoGenerador IS NULL OR g.type = :tipoGenerador) 
        AND (:zona IS NULL OR z.name = :zona)
        GROUP BY g.generatorId, g.name
        ORDER BY SUM(s.quantity) DESC
        LIMIT 1
        """)
    String getTopGeneratorByQuantity(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            @Param("tipoGenerador") String tipoGenerador,
            @Param("zona") String zona
    );
}
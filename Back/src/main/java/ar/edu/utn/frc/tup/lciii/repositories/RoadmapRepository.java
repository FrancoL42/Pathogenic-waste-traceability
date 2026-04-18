package ar.edu.utn.frc.tup.lciii.repositories;

import ar.edu.utn.frc.tup.lciii.entities.RoadmapEntity;
import ar.edu.utn.frc.tup.lciii.entities.SealEntity;
import ar.edu.utn.frc.tup.lciii.models.RoadmapStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoadmapRepository extends JpaRepository<RoadmapEntity, Long> {

    // ===================================================
    // CONSULTAS EXISTENTES MEJORADAS
    // ===================================================

    // Roadmaps con detalles cargados
    @Query("SELECT r FROM RoadmapEntity r LEFT JOIN FETCH r.details d LEFT JOIN FETCH d.generatorEntity WHERE r.roadmapId = :id")
    Optional<RoadmapEntity> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT r FROM RoadmapEntity r LEFT JOIN FETCH r.details d LEFT JOIN FETCH d.generatorEntity")
    List<RoadmapEntity> findAllWithDetails();

    // ===================================================
    // NUEVAS CONSULTAS PARA EMPLEADOS
    // ===================================================

    // Roadmaps activas de un empleado específico (por ID de empleado)
    @Query("""
    SELECT DISTINCT r FROM RoadmapEntity r 
    LEFT JOIN FETCH r.details d 
    LEFT JOIN FETCH d.generatorEntity g
    JOIN EmployeesEntity e ON r.employee = e.name
    WHERE e.id = :employeeId 
    AND DATE(r.collectDate) = CURRENT_DATE
    AND r.status != ar.edu.utn.frc.tup.lciii.models.RoadmapStatus.CERRADA
    ORDER BY r.collectDate
    """)
    List<RoadmapEntity> findAllActiveByEmployee(@Param("employeeId") Long employeeId);

    // Roadmap específica de un empleado
    @Query("""
        SELECT r FROM RoadmapEntity r 
        LEFT JOIN FETCH r.details d 
        LEFT JOIN FETCH d.generatorEntity g
        JOIN EmployeesEntity e ON r.employee = e.name
        WHERE e.id = :employeeId AND r.roadmapId = :roadmapId
        """)
    Optional<RoadmapEntity> findSpecificRoadmap(@Param("employeeId") Long employeeId, @Param("roadmapId") Long roadmapId);

    // ===================================================
    // CONSULTAS PARA VALIDACIÓN DE QR
    // ===================================================

    // Verificar si un generador está en la hoja de ruta de un empleado
    @Query("""
        SELECT COUNT(d) > 0 FROM RoadmapDetailEntity d
        JOIN d.roadmapEntity r
        JOIN EmployeesEntity e ON r.employee = e.name
        WHERE e.id = :employeeId 
        AND d.generatorEntity.generatorId = :generatorId
        AND r.roadmapId = :roadmapId
        """)
    boolean existsGeneratorInEmployeeRoadmap(@Param("employeeId") Long employeeId,
                                             @Param("generatorId") Long generatorId,
                                             @Param("roadmapId") Long roadmapId);

    // ===================================================
    // CONSULTAS PARA OPTIMIZACIÓN DE RUTAS
    // ===================================================

    // Roadmaps que necesitan optimización (sin coordenadas válidas)
    @Query("""
        SELECT r FROM RoadmapEntity r 
        JOIN r.details d 
        JOIN d.generatorEntity g
        WHERE g.latitude IS NULL OR g.longitude IS NULL
        """)
    List<RoadmapEntity> findRoadmapsNeedingGeocoding();

    // Roadmaps listas para optimización (con todas las coordenadas)
    @Query("""
        SELECT r FROM RoadmapEntity r 
        LEFT JOIN FETCH r.details d 
        LEFT JOIN FETCH d.generatorEntity g
        WHERE r.roadmapId NOT IN (
            SELECT DISTINCT r2.roadmapId FROM RoadmapEntity r2 
            JOIN r2.details d2 
            JOIN d2.generatorEntity g2
            WHERE g2.latitude IS NULL OR g2.longitude IS NULL
        )
        """)
    List<RoadmapEntity> findRoadmapsReadyForOptimization();

    // Roadmaps por zona con coordenadas válidas
    @Query("""
        SELECT r FROM RoadmapEntity r 
        LEFT JOIN FETCH r.details d 
        LEFT JOIN FETCH d.generatorEntity g
        WHERE r.zone = :zone 
        AND g.latitude IS NOT NULL AND g.longitude IS NOT NULL
        """)
    List<RoadmapEntity> findByZoneWithValidCoordinates(@Param("zone") String zone);

    // ===================================================
    // ESTADÍSTICAS Y REPORTES
    // ===================================================

    // Contar roadmaps por empleado
    @Query("SELECT COUNT(r) FROM RoadmapEntity r WHERE r.employee = :employeeName")
    long countByEmployee(@Param("employeeName") String employeeName);

    // Roadmaps de hoy
    @Query("SELECT r FROM RoadmapEntity r WHERE DATE(r.collectDate) = CURRENT_DATE")
    List<RoadmapEntity> findTodaysRoadmaps();

    // Roadmaps pendientes (futuras)
    @Query("SELECT r FROM RoadmapEntity r WHERE r.collectDate > CURRENT_TIMESTAMP")
    List<RoadmapEntity> findPendingRoadmaps();
    // Hojas de ruta completadas listas para cerrar (por empleado)
    @Query("""
    SELECT DISTINCT r FROM RoadmapEntity r 
    LEFT JOIN FETCH r.details d 
    LEFT JOIN FETCH d.generatorEntity g
    JOIN EmployeesEntity e ON r.employee = e.name
    WHERE e.id = :employeeId 
    AND r.status = :status
    AND DATE(r.collectDate) = CURRENT_DATE
    ORDER BY r.collectDate DESC
    """)
    List<RoadmapEntity> findByEmployeeAndStatus(@Param("employeeId") Long employeeId,
                                                @Param("status") RoadmapStatus status);

    // Obtener todos los precintos recolectados de una hoja de ruta
    @Query("""
    SELECT s FROM SealEntity s 
JOIN s.generatorEntity g 
JOIN g.details d 
JOIN d.roadmapEntity r 
WHERE r.roadmapId = :roadmapId 
AND s.state = ar.edu.utn.frc.tup.lciii.models.SealStatus.RECOLECTADO
ORDER BY g.name, s.sealNumber
""")
    List<SealEntity> findCollectedSealsByRoadmap(@Param("roadmapId") Long roadmapId);

    // Contar precintos recolectados en una hoja de ruta
    @Query("""
    SELECT COUNT(s) FROM SealEntity s 
    JOIN s.generatorEntity g 
    JOIN g.details d 
    JOIN d.roadmapEntity r 
    WHERE r.roadmapId = :roadmapId 
    AND s.state = 'RECOLECTADO'
    """)
    Long countCollectedSealsByRoadmap(@Param("roadmapId") Long roadmapId);

    // Verificar si una hoja de ruta está lista para cerrar
    @Query("""
    SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END 
    FROM SealEntity s 
    JOIN s.generatorEntity g 
    JOIN g.details d 
    JOIN d.roadmapEntity r 
    WHERE r.roadmapId = :roadmapId 
    AND s.state = 'RECOLECTADO'
    """)
    boolean hasCollectedSeals(@Param("roadmapId") Long roadmapId);
    @Query("""
SELECT DISTINCT r FROM RoadmapEntity r 
LEFT JOIN FETCH r.details d 
LEFT JOIN FETCH d.generatorEntity g
JOIN EmployeesEntity e ON r.employee = e.name
WHERE e.id = :employeeId 
AND r.status = 'COMPLETADA'
AND DATE(r.collectDate) = CURRENT_DATE
ORDER BY r.collectDate DESC
""")
    List<RoadmapEntity> findCompletedRoadmapsReadyToClose(@Param("employeeId") Long employeeId);

    // Verificar si una ruta puede ser cerrada
    @Query("""
SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END 
FROM RoadmapEntity r 
JOIN EmployeesEntity e ON r.employee = e.name
WHERE e.id = :employeeId 
AND r.roadmapId = :roadmapId 
AND r.status IN (ar.edu.utn.frc.tup.lciii.models.RoadmapStatus.EN_PROGRESO, ar.edu.utn.frc.tup.lciii.models.RoadmapStatus.COMPLETADA)
""")
    boolean canRoadmapBeClosed(@Param("employeeId") Long employeeId, @Param("roadmapId") Long roadmapId);

    // 🆕 NUEVA: Estadísticas por estado
    @Query("""
    SELECT r.status, COUNT(r) 
    FROM RoadmapEntity r 
    WHERE DATE(r.collectDate) = CURRENT_DATE
    GROUP BY r.status
    """)
    List<Object[]> countRoadmapsByStatusToday();

    // 🆕 NUEVA: Rutas listas para completar (con precintos recolectados)
    @Query("""
    SELECT DISTINCT r FROM RoadmapEntity r 
    LEFT JOIN FETCH r.details d 
    LEFT JOIN FETCH d.generatorEntity g
    WHERE r.status = 'EN_PROGRESO'
    AND EXISTS (
        SELECT s FROM SealEntity s 
        JOIN s.generatorEntity gen 
        JOIN gen.details det 
        WHERE det.roadmapEntity = r 
        AND s.state = 'RECOLECTADO'
    )
    ORDER BY r.collectDate
    """)
    List<RoadmapEntity> findRoadmapsReadyToComplete();
    @Query("""
SELECT DISTINCT r FROM RoadmapEntity r 
LEFT JOIN FETCH r.details d 
LEFT JOIN FETCH d.generatorEntity g
JOIN EmployeesEntity e ON r.employee = e.name
WHERE e.id = :employeeId 
AND r.status IN (ar.edu.utn.frc.tup.lciii.models.RoadmapStatus.EN_PROGRESO, ar.edu.utn.frc.tup.lciii.models.RoadmapStatus.COMPLETADA)
AND DATE(r.collectDate) = CURRENT_DATE
ORDER BY r.collectDate DESC
""")
    List<RoadmapEntity> findRoadmapsReadyToClose(@Param("employeeId") Long employeeId);
    @Query("""
SELECT DISTINCT r FROM RoadmapEntity r 
LEFT JOIN FETCH r.details d 
LEFT JOIN FETCH d.generatorEntity g
JOIN EmployeesEntity e ON r.employee = e.name
WHERE e.id = :employeeId 
AND r.status = ar.edu.utn.frc.tup.lciii.models.RoadmapStatus.EN_PROGRESO
AND DATE(r.collectDate) = CURRENT_DATE
ORDER BY r.collectDate DESC
""")
    List<RoadmapEntity> findActiveRoadmapsToClose(@Param("employeeId") Long employeeId);
}
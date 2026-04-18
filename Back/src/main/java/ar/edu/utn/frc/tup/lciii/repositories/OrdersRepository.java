package ar.edu.utn.frc.tup.lciii.repositories;

import ar.edu.utn.frc.tup.lciii.entities.OrdersEntity;
import ar.edu.utn.frc.tup.lciii.entities.ZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrdersRepository extends JpaRepository<OrdersEntity, Long> {
    List<OrdersEntity> findAllByZoneEntity(ZoneEntity zoneEntity);
    /**
     * Busca órdenes entre fechas
     */
    List<OrdersEntity> findByScheduledDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Busca órdenes entre fechas con estado específico
     */
    List<OrdersEntity> findByScheduledDateBetweenAndState(
            LocalDateTime startDate, LocalDateTime endDate, String state);

    /**
     * Busca órdenes entre fechas de una zona específica
     */
    List<OrdersEntity> findByScheduledDateBetweenAndZoneEntity_Name(
            LocalDateTime startDate, LocalDateTime endDate, String zoneName);

    /**
     * Busca órdenes entre fechas con estado y zona específicos
     */
    List<OrdersEntity> findByScheduledDateBetweenAndStateAndZoneEntity_Name(
            LocalDateTime startDate, LocalDateTime endDate, String state, String zoneName);

    /**
     * Cuenta total de órdenes por zona
     */
    @Query("SELECT COUNT(o) FROM OrdersEntity o WHERE o.zoneEntity.name = :zoneName")
    Long countByZoneName(@Param("zoneName") String zoneName);

    /**
     * Cuenta órdenes por zona y estado
     */
    @Query("SELECT COUNT(o) FROM OrdersEntity o WHERE o.zoneEntity.name = :zoneName AND o.state = :state")
    Long countByZoneNameAndState(@Param("zoneName") String zoneName, @Param("state") String state);

    /**
     * Suma total de bolsas por zona
     */
    @Query("SELECT COALESCE(SUM(o.countBags), 0) FROM OrdersEntity o WHERE o.zoneEntity.name = :zoneName")
    Integer sumBagsByZoneName(@Param("zoneName") String zoneName);

    /**
     * Obtiene estadísticas agregadas por zona
     */
    @Query("""
        SELECT o.zoneEntity.name as zoneName,
               COUNT(o) as totalOrders,
               SUM(CASE WHEN o.state = 'PENDIENTE' THEN 1 ELSE 0 END) as pendingOrders,
               SUM(CASE WHEN o.state = 'COMPLETADO' THEN 1 ELSE 0 END) as completedOrders,
               COALESCE(SUM(o.countBags), 0) as totalBags,
               MAX(o.scheduledDate) as lastOrderDate
        FROM OrdersEntity o
        WHERE o.scheduledDate BETWEEN :startDate AND :endDate
        GROUP BY o.zoneEntity.name
        ORDER BY COUNT(o) DESC
        """)
    List<Object[]> getZoneStatistics(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Obtiene estadísticas agregadas por zona con filtro de estado
     */
    @Query("""
        SELECT o.zoneEntity.name as zoneName,
               COUNT(o) as totalOrders,
               SUM(CASE WHEN o.state = 'PENDIENTE' THEN 1 ELSE 0 END) as pendingOrders,
               SUM(CASE WHEN o.state = 'COMPLETADO' THEN 1 ELSE 0 END) as completedOrders,
               COALESCE(SUM(o.countBags), 0) as totalBags,
               MAX(o.scheduledDate) as lastOrderDate
        FROM OrdersEntity o
        WHERE o.scheduledDate BETWEEN :startDate AND :endDate
        AND o.state = :state
        GROUP BY o.zoneEntity.name
        ORDER BY COUNT(o) DESC
        """)
    List<Object[]> getZoneStatisticsByState(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate,
                                            @Param("state") String state);

    /**
     * Obtiene estadísticas de una zona específica
     */
    @Query("""
        SELECT o.zoneEntity.name as zoneName,
               COUNT(o) as totalOrders,
               SUM(CASE WHEN o.state = 'PENDIENTE' THEN 1 ELSE 0 END) as pendingOrders,
               SUM(CASE WHEN o.state = 'COMPLETADO' THEN 1 ELSE 0 END) as completedOrders,
               COALESCE(SUM(o.countBags), 0) as totalBags,
               MAX(o.scheduledDate) as lastOrderDate
        FROM OrdersEntity o
        WHERE o.scheduledDate BETWEEN :startDate AND :endDate
        AND o.zoneEntity.name = :zoneName
        GROUP BY o.zoneEntity.name
        """)
    List<Object[]> getSpecificZoneStatistics(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate,
                                             @Param("zoneName") String zoneName);

    /**
     * Busca la última orden de cada zona
     */
    @Query("""
        SELECT o FROM OrdersEntity o
        WHERE o.scheduledDate = (
            SELECT MAX(o2.scheduledDate) 
            FROM OrdersEntity o2 
            WHERE o2.zoneEntity.zoneId = o.zoneEntity.zoneId
        )
        """)
    List<OrdersEntity> findLatestOrderByZone();

    /**
     * Obtiene el promedio de días entre creación y programación por zona
     */
    @Query("""
        SELECT o.zoneEntity.name,
               AVG(DATEDIFF(o.scheduledDate, CURRENT_DATE)) as avgDays
        FROM OrdersEntity o
        WHERE o.state = 'COMPLETADO'
        AND o.scheduledDate BETWEEN :startDate AND :endDate
        GROUP BY o.zoneEntity.name
        """)
    List<Object[]> getAverageProcessingDaysByZone(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);
    @Query("""
        SELECT o FROM OrdersEntity o 
        WHERE o.generatorEntity.generatorId = :generatorId 
        AND o.state IN ('PENDIENTE', 'EN_PROCESO')
        ORDER BY o.scheduledDate DESC
        LIMIT 1
        """)
    Optional<OrdersEntity> findActiveOrderByGeneratorId(@Param("generatorId") Long generatorId);
    @Query("""
        SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END 
        FROM OrdersEntity o 
        WHERE o.generatorEntity.generatorId = :generatorId 
        AND o.state IN ('PENDIENTE', 'EN_PROCESO')
        """)
    boolean hasActiveOrder(@Param("generatorId") Long generatorId);
}

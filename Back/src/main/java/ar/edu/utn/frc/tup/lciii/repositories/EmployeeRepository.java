package ar.edu.utn.frc.tup.lciii.repositories;

import ar.edu.utn.frc.tup.lciii.entities.EmployeesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<EmployeesEntity, Long> {
    Boolean existsByName(String name);
    EmployeesEntity findByName( String name);
    // Buscar empleados activos
    List<EmployeesEntity> findByState(String state);

    List<EmployeesEntity> findByStateOrderByNameAsc(String state);


    Optional<EmployeesEntity> findByNameAndState(String name, String state);

    // Buscar empleados con pedidos asignados
    @Query("SELECT DISTINCT e FROM EmployeesEntity e " +
            "JOIN OrdersEntity o ON o.employeeEntity.id = e.id " +
            "WHERE o.state IN ('PENDIENTE', 'EN_PROCESO')")
    List<EmployeesEntity> findEmployeesWithActiveOrders();

    // Contar pedidos asignados por empleado
    @Query("SELECT e.id, e.name, COUNT(o) as orderCount " +
            "FROM EmployeesEntity e " +
            "LEFT JOIN OrdersEntity o ON o.employeeEntity.id = e.id " +
            "WHERE e.state = 'ACTIVO' " +
            "AND (o.state IS NULL OR o.state IN ('PENDIENTE', 'EN_PROCESO')) " +
            "GROUP BY e.id, e.name " +
            "ORDER BY orderCount ASC")
    List<Object[]> findEmployeesWithOrderCount();



    // Empleados disponibles (activos sin pedidos en proceso)
    @Query("SELECT e FROM EmployeesEntity e " +
            "WHERE e.state = 'ACTIVO' " +
            "AND e.id NOT IN (" +
            "    SELECT DISTINCT o.employeeEntity.id " +
            "    FROM OrdersEntity o " +
            "    WHERE o.employeeEntity.id IS NOT NULL " +
            "    AND o.state = 'EN_PROCESO'" +
            ")")
    List<EmployeesEntity> findAvailableEmployees();

    // Buscar por parte del nombre (para autocompletado)
    @Query("SELECT e FROM EmployeesEntity e " +
            "WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :namePart, '%')) " +
            "AND e.state = 'ACTIVO'")
    List<EmployeesEntity> findByNameContainingIgnoreCase(@Param("namePart") String namePart);
    @Query("SELECT e FROM EmployeesEntity e WHERE e.user.id = :userId")
    Optional<EmployeesEntity> findByUserId(@Param("userId") Long userId);
}

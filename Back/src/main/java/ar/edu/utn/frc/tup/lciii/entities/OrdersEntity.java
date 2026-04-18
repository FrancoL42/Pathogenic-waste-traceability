package ar.edu.utn.frc.tup.lciii.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "Pedidos_Retiro")
public class OrdersEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido_retiro", nullable = false)
    private Long id;

    @Column(name = "estado")
    private String state;

    @ManyToOne
    @JoinColumn(name = "zoneId", nullable = false)
    private ZoneEntity zoneEntity;

    @ManyToOne
    @JoinColumn(name = "generatorId", nullable = false)
    private GeneratorEntity generatorEntity;

    @ManyToOne
    @JoinColumn(name = "empleado_id", nullable = true)
    private EmployeesEntity employeeEntity;

    @Column(name = "fecha_programada")
    private LocalDateTime scheduledDate;
    @ManyToOne
    @JoinColumn(name = "roadmap_id", nullable = true)
    private RoadmapEntity roadmapEntity;
    @Column(name = "cantidad_bolsas")
    private Integer countBags;

}

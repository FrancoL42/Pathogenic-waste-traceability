package ar.edu.utn.frc.tup.lciii.entities;

import ar.edu.utn.frc.tup.lciii.models.ContainerStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Containers")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContainersEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long containerId;
    @OneToMany(mappedBy = "containersEntity", fetch = FetchType.LAZY)
    private List<SealEntity> seals;
    @Column(name = "peso_maximo", nullable = false)
    private Double pesoMaximo;

    @Column(name = "peso_actual", nullable = false)
    private Double pesoActual = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private ContainerStatus estado = ContainerStatus.ABIERTO;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();



}

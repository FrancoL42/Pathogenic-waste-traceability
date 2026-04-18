package ar.edu.utn.frc.tup.lciii.entities;

import ar.edu.utn.frc.tup.lciii.models.SealStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "Precintos")
@Data
public class SealEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_precinto", nullable = false)
    private Long sealId;

    @ManyToOne
    @JoinColumn(name = "generatorId", nullable = true)
    private GeneratorEntity generatorEntity;

    @Column(name = "nro_precinto")
    private String sealNumber;
    @Enumerated(EnumType.STRING)  // ⬅️ CAMBIAR ESTO
    @Column(name = "state")
    private SealStatus state;// DISPONIBLE, OCUPADO, RECOLECTADO, EN_TRATAMIENTO, TRATADO
    // 🆕 NUEVO CAMPO PARA QR
    @Column(name = "qr_content")
    private String qrContent;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "containerId", nullable = true)
    private ContainersEntity containersEntity;
    @Column
    private Double peso;
    @Column(name = "fecha_tratamiento")
    private LocalDate fechaTratamiento;
}


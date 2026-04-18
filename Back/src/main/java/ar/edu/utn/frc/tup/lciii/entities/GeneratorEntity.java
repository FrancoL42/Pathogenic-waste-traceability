package ar.edu.utn.frc.tup.lciii.entities;

import ar.edu.utn.frc.tup.lciii.models.RegisterState;
import ar.edu.utn.frc.tup.lciii.models.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Generadores")
@Data
public class GeneratorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long generatorId;

    @Column(name = "nombre")
    private String name;

    @Column(name = "fecha_alta")
    private LocalDateTime entryDate;

    @Column(name = "fecha_baja")
    private LocalDateTime exitDate;

    @Column
    private String email;

    @Column(name = "contacto")
    private String contact;

    @Column(name = "tipo")
    private String type;

    @Column(name = "domicilio")
    private String address;
    @Column(name = "latitud")
    private Double latitude;
    @Column(name = "longitud")
    private Double longitude;


    @Column(name = "estado")
    @Enumerated(EnumType.STRING)
    private RegisterState state;


    @OneToMany(mappedBy = "generatorEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<RoadmapDetailEntity> details;

    @OneToMany(mappedBy = "generatorEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<SealEntity> seals;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", referencedColumnName = "id")
    private ZoneEntity zone;
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private UserEntity user;
}


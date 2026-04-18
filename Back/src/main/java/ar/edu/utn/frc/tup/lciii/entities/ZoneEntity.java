package ar.edu.utn.frc.tup.lciii.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "Zonas")
@Data
public class ZoneEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long zoneId;
    @Column (name = "nombre")
    private String name;
    // 🆕 RELACIÓN BIDIRECCIONAL (OPCIONAL)
    // Solo si necesitas consultar "todos los generadores de una zona"
    @OneToMany(mappedBy = "zone", fetch = FetchType.LAZY)
    @JsonIgnore // Evita loops infinitos en JSON
    private List<GeneratorEntity> generators;
}

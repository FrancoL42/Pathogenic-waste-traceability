package ar.edu.utn.frc.tup.lciii.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Certificates")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CertificateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long certificateId;

    @ManyToOne
    @JoinColumn(name = "generatorId", nullable = false)
    private GeneratorEntity generatorEntity;

    @Column(name = "numero_certificado", unique = true, nullable = false)
    private String numeroCertificado;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "fecha_tratamiento", nullable = false)
    private LocalDate fechaTratamiento;

    @Column(name = "cantidad_precintos", nullable = false)
    private Integer cantidadPrecintos;

    @Column(name = "peso_total", nullable = false)
    private Double pesoTotal;

    @Lob
    @Column(name = "contenido_pdf")
    private String contenidoPdf;
}

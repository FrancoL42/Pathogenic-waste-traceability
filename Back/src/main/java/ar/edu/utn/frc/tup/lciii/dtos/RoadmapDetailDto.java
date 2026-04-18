package ar.edu.utn.frc.tup.lciii.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoadmapDetailDto {
    private Long id;

    // Información del generador
    private Long generatorId;
    private String generatorName;
    private String generatorAddress;
    private Double latitude;
    private Double longitude;
    private String generatorType;
    private String generatorContact;
    private String generatorEmail;

    // Información del precinto (si existe)
    private String sealNumber;

    // Para mantener compatibilidad con tu código existente
    private GeneratorDto generator; // Opcional, por si lo usas en otros lados
}

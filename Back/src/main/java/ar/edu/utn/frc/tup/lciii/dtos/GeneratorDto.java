package ar.edu.utn.frc.tup.lciii.dtos;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeneratorDto {
    private Long generatorId;
    private String name;
    private String email;
    private String contact;
    private String address;
    private String type;
    private String state;
    private List<PrecintoDto> precintos;
    private Double latitude;
    private Double longitude;
    private Boolean acceptTerms;
    private String zona;
}

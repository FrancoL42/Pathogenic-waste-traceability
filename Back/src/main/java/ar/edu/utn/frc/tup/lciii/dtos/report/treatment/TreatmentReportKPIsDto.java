package ar.edu.utn.frc.tup.lciii.dtos.report.treatment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TreatmentReportKPIsDto {
    private Integer totalTratamientos;
    private Integer totalBolsas;
    private Double pesoTotal;
    private Double promedioPesoPorBolsa;
    private String generadorTop;
    private String diaMasTratamientos;
}

package ar.edu.utn.frc.tup.lciii.dtos.report.treatment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TreatmentReportDto {
    private LocalDate fechaTratamiento;
    private String generador;
    private String tipoGenerador;
    private String zona;
    private Integer cantidadBolsas;
    private Double pesoTotal;
}

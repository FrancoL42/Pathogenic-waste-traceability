package ar.edu.utn.frc.tup.lciii.dtos.report.treatment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TreatmentReportFiltersDto {
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String tipoGenerador; // "Público" o "Privado"
    private String zona;
}
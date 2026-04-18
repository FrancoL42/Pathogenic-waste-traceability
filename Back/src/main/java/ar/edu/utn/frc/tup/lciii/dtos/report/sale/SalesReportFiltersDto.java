package ar.edu.utn.frc.tup.lciii.dtos.report.sale;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportFiltersDto {
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String tipoGenerador;
    private String zona;
}
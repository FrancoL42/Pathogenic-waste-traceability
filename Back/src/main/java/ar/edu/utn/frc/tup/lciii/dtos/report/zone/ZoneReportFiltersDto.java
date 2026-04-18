package ar.edu.utn.frc.tup.lciii.dtos.report.zone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate; /**
 * DTO para los filtros del reporte
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneReportFiltersDto {
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String estado;
    private String zona;
}

package ar.edu.utn.frc.tup.lciii.dtos.report.treatment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TreatmentReportResponseDto {
    private boolean success;
    private String message;
    private List<TreatmentReportDto> data;
    private TreatmentReportKPIsDto kpis;
}

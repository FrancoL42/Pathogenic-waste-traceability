package ar.edu.utn.frc.tup.lciii.dtos.report.sale;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportResponseDto {
    private boolean success;
    private String message;
    private List<SalesReportDto> data;
    private SalesReportKPIsDto kpis;
}

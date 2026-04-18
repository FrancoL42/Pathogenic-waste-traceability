package ar.edu.utn.frc.tup.lciii.dtos.report.zone;

import ar.edu.utn.frc.tup.lciii.dtos.ZoneReportDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List; /**
 * DTO para la respuesta completa del reporte
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneReportResponseDto {
    private Boolean success;
    private List<ZoneReportDto> data;
    private ZoneReportKpisDto kpis;
    private String message;
}

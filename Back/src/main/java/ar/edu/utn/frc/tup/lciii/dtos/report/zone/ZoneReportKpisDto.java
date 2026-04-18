package ar.edu.utn.frc.tup.lciii.dtos.report.zone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor; /**
 * DTO para los KPIs del reporte
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneReportKpisDto {
    private Integer totalSolicitudesGlobal;
    private String zonaConMasSolicitudes;
    private Integer promedioSolicitudesPorZona;
    private Double porcentajeCompletadoGeneral;
    private Integer totalBolsasRecolectadas;
    private Double tiempoPromedioRespuesta;
}

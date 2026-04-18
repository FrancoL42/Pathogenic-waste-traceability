package ar.edu.utn.frc.tup.lciii.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO para un elemento del reporte de zonas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneReportDto {
    private String zona;
    private Integer totalSolicitudes;
    private Integer solicitudesPendientes;
    private Integer solicitudesCompletadas;
    private Integer totalBolsas;
    private LocalDate fechaUltimaSolicitud;
    private Double promedioDiasProcesamiento;
    private Double porcentajeCompletado;
}


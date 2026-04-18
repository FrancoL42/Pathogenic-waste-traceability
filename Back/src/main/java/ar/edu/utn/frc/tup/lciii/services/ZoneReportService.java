package ar.edu.utn.frc.tup.lciii.services;

import ar.edu.utn.frc.tup.lciii.dtos.ZoneReportDto;
import ar.edu.utn.frc.tup.lciii.dtos.report.zone.ZoneReportKpisDto;
import ar.edu.utn.frc.tup.lciii.dtos.report.zone.ZoneReportResponseDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
public interface ZoneReportService {
    ZoneReportResponseDto generateZoneReport(LocalDate fechaInicio, LocalDate fechaFin,
                                             String estado, String zona);

    ZoneReportKpisDto calculateKPIs(LocalDate fechaInicio, LocalDate fechaFin,
                                    String estado, String zona);

    ZoneReportDto getZoneDetails(String zoneName, LocalDate fechaInicio, LocalDate fechaFin, String estado);

    List<String> getAvailableZones();

    byte[] exportToExcel(LocalDate fechaInicio, LocalDate fechaFin, String estado, String zona)
            throws IOException;

    byte[] exportToPDF(LocalDate fechaInicio, LocalDate fechaFin, String estado, String zona);
}

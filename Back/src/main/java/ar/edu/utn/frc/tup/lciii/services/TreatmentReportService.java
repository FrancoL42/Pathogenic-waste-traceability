package ar.edu.utn.frc.tup.lciii.services;

import ar.edu.utn.frc.tup.lciii.dtos.report.treatment.TreatmentReportFiltersDto;
import ar.edu.utn.frc.tup.lciii.dtos.report.treatment.TreatmentReportResponseDto;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface TreatmentReportService {
    TreatmentReportResponseDto generateTreatmentReport(TreatmentReportFiltersDto filters);
    List<String> getAvailableZones();
    ByteArrayResource exportToExcel(TreatmentReportFiltersDto filters);
    ByteArrayResource exportToPDF(TreatmentReportFiltersDto filters);
}

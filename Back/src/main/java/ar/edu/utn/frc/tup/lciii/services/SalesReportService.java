package ar.edu.utn.frc.tup.lciii.services;

import ar.edu.utn.frc.tup.lciii.dtos.report.sale.SalesReportFiltersDto;
import ar.edu.utn.frc.tup.lciii.dtos.report.sale.SalesReportResponseDto;
import org.springframework.core.io.ByteArrayResource;

import java.util.List;

public interface SalesReportService {

    /**
     * Genera el reporte de ventas de bolsas por generador
     */
    SalesReportResponseDto generateSalesReport(SalesReportFiltersDto filters);

    /**
     * Obtiene las zonas disponibles para el filtro
     */
    List<String> getAvailableZones();

    /**
     * Exporta el reporte a Excel
     */
    ByteArrayResource exportToExcel(SalesReportFiltersDto filters);

    /**
     * Exporta el reporte a PDF
     */
    ByteArrayResource exportToPDF(SalesReportFiltersDto filters);
}
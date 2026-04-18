package ar.edu.utn.frc.tup.lciii.controllers;

import ar.edu.utn.frc.tup.lciii.dtos.ZoneReportDto;

import ar.edu.utn.frc.tup.lciii.dtos.report.zone.ZoneReportKpisDto;
import ar.edu.utn.frc.tup.lciii.dtos.report.zone.ZoneReportResponseDto;
import ar.edu.utn.frc.tup.lciii.services.ZoneReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reporte-zonas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ZoneReportController {

    private final ZoneReportService zoneReportService;

    /**
     * Obtiene el reporte completo de zonas con filtros
     */
    @GetMapping
    public ResponseEntity<ZoneReportResponseDto> getZoneReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String zona) {

        try {
            ZoneReportResponseDto response = zoneReportService.generateZoneReport(
                    fechaInicio, fechaFin, estado, zona);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ZoneReportResponseDto errorResponse = ZoneReportResponseDto.builder()
                    .success(false)
                    .message("Error al generar el reporte: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Obtiene solo los KPIs del reporte
     */
    @GetMapping("/kpis")
    public ResponseEntity<ZoneReportKpisDto> getZoneKPIs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String zona) {

        try {
            ZoneReportKpisDto kpis = zoneReportService.calculateKPIs(
                    fechaInicio, fechaFin, estado, zona);
            return ResponseEntity.ok(kpis);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Exporta el reporte a Excel
     */
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String zona) {

        try {
            byte[] excelFile = zoneReportService.exportToExcel(
                    fechaInicio, fechaFin, estado, zona);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    "reporte_zonas_" + LocalDate.now() + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelFile);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Exporta el reporte a PDF
     */
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportToPDF(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String zona) {

        try {
            byte[] pdfFile = zoneReportService.exportToPDF(
                    fechaInicio, fechaFin, estado, zona);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "reporte_zonas_" + LocalDate.now() + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfFile);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene detalles específicos de una zona
     */
    @GetMapping("/detalle")
    public ResponseEntity<ZoneReportDto> getZoneDetails(
            @RequestParam String zona,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) String estado) {

        try {
            ZoneReportDto zoneDetails = zoneReportService.getZoneDetails(
                    zona, fechaInicio, fechaFin, estado);
            return ResponseEntity.ok(zoneDetails);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene la lista de nombres de zonas disponibles
     */
    @GetMapping("/zonas/nombres")
    public ResponseEntity<List<String>> getAvailableZones() {
        try {
            List<String> zones = zoneReportService.getAvailableZones();
            return ResponseEntity.ok(zones);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
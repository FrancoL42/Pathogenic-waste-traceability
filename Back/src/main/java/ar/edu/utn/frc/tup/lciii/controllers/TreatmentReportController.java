package ar.edu.utn.frc.tup.lciii.controllers;

import ar.edu.utn.frc.tup.lciii.dtos.report.treatment.TreatmentReportFiltersDto;
import ar.edu.utn.frc.tup.lciii.dtos.report.treatment.TreatmentReportResponseDto;
import ar.edu.utn.frc.tup.lciii.services.TreatmentReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/reports/treatments")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Treatment Reports", description = "API para generar reportes de tratamientos realizados")
public class TreatmentReportController {

    private final TreatmentReportService treatmentReportService;

    @GetMapping("/generate")
    @Operation(summary = "Generar reporte de tratamientos",
            description = "Genera un reporte detallado de los tratamientos realizados con filtros opcionales")
    public ResponseEntity<TreatmentReportResponseDto> generateTreatmentReport(
            @Parameter(description = "Fecha de inicio (formato: yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,

            @Parameter(description = "Fecha de fin (formato: yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,

            @Parameter(description = "Tipo de generador: Público o Privado")
            @RequestParam(required = false) String tipoGenerador,

            @Parameter(description = "Zona específica para filtrar")
            @RequestParam(required = false) String zona) {

        try {
            log.info("Generando reporte de tratamientos con filtros: fechaInicio={}, fechaFin={}, tipo={}, zona={}",
                    fechaInicio, fechaFin, tipoGenerador, zona);

            TreatmentReportFiltersDto filters = new TreatmentReportFiltersDto(
                    fechaInicio, fechaFin, tipoGenerador, zona);

            TreatmentReportResponseDto response = treatmentReportService.generateTreatmentReport(filters);

            if (response.isSuccess()) {
                log.info("Reporte de tratamientos generado exitosamente con {} registros",
                        response.getData().size());
                return ResponseEntity.ok(response);
            } else {
                log.warn("No se pudo generar el reporte: {}", response.getMessage());
                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            log.error("Error al generar reporte de tratamientos", e);
            TreatmentReportResponseDto errorResponse = new TreatmentReportResponseDto();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Error interno del servidor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/zones")
    @Operation(summary = "Obtener zonas disponibles",
            description = "Retorna la lista de zonas donde se han realizado tratamientos")
    public ResponseEntity<List<String>> getAvailableZones() {
        try {
            log.info("Obteniendo zonas disponibles para reporte de tratamientos");
            List<String> zones = treatmentReportService.getAvailableZones();
            log.info("Se encontraron {} zonas disponibles", zones.size());
            return ResponseEntity.ok(zones);
        } catch (Exception e) {
            log.error("Error al obtener zonas disponibles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/export/excel")
    @Operation(summary = "Exportar reporte a Excel",
            description = "Genera y descarga el reporte de tratamientos en formato Excel")
    public ResponseEntity<ByteArrayResource> exportToExcel(
            @Parameter(description = "Fecha de inicio (formato: yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,

            @Parameter(description = "Fecha de fin (formato: yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,

            @Parameter(description = "Tipo de generador: Público o Privado")
            @RequestParam(required = false) String tipoGenerador,

            @Parameter(description = "Zona específica para filtrar")
            @RequestParam(required = false) String zona) {

        try {
            log.info("Exportando reporte de tratamientos a Excel con filtros: fechaInicio={}, fechaFin={}, tipo={}, zona={}",
                    fechaInicio, fechaFin, tipoGenerador, zona);

            TreatmentReportFiltersDto filters = new TreatmentReportFiltersDto(
                    fechaInicio, fechaFin, tipoGenerador, zona);

            ByteArrayResource resource = treatmentReportService.exportToExcel(filters);

            // Crear nombre del archivo con fecha y filtros
            String fileName = buildFileName("Reporte_Tratamientos", filters, "xlsx");

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            log.info("Archivo Excel generado exitosamente: {}", fileName);
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(resource.contentLength())
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(resource);

        } catch (Exception e) {
            log.error("Error al exportar reporte a Excel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/export/pdf")
    @Operation(summary = "Exportar reporte a PDF",
            description = "Genera y descarga el reporte de tratamientos en formato PDF")
    public ResponseEntity<ByteArrayResource> exportToPDF(
            @Parameter(description = "Fecha de inicio (formato: yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,

            @Parameter(description = "Fecha de fin (formato: yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,

            @Parameter(description = "Tipo de generador: Público o Privado")
            @RequestParam(required = false) String tipoGenerador,

            @Parameter(description = "Zona específica para filtrar")
            @RequestParam(required = false) String zona) {

        try {
            log.info("Exportando reporte de tratamientos a PDF con filtros: fechaInicio={}, fechaFin={}, tipo={}, zona={}",
                    fechaInicio, fechaFin, tipoGenerador, zona);

            TreatmentReportFiltersDto filters = new TreatmentReportFiltersDto(
                    fechaInicio, fechaFin, tipoGenerador, zona);

            ByteArrayResource resource = treatmentReportService.exportToPDF(filters);

            // Crear nombre del archivo con fecha y filtros
            String fileName = buildFileName("Reporte_Tratamientos", filters, "pdf");

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/pdf");

            log.info("Archivo PDF generado exitosamente: {}", fileName);
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(resource.contentLength())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);

        } catch (Exception e) {
            log.error("Error al exportar reporte a PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Construye el nombre del archivo basado en filtros aplicados
     */
    private String buildFileName(String baseName, TreatmentReportFiltersDto filters, String extension) {
        StringBuilder fileName = new StringBuilder(baseName);

        if (filters.getFechaInicio() != null || filters.getFechaFin() != null) {
            fileName.append("_");
            if (filters.getFechaInicio() != null) {
                fileName.append(filters.getFechaInicio().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }
            fileName.append("_a_");
            if (filters.getFechaFin() != null) {
                fileName.append(filters.getFechaFin().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }
        }

        if (filters.getTipoGenerador() != null && !filters.getTipoGenerador().isEmpty()) {
            fileName.append("_").append(filters.getTipoGenerador().replace(" ", "_"));
        }

        if (filters.getZona() != null && !filters.getZona().isEmpty()) {
            fileName.append("_").append(filters.getZona().replace(" ", "_"));
        }

        // Agregar timestamp para evitar conflictos
        fileName.append("_").append(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        fileName.append(".").append(extension);

        return fileName.toString();
    }
}
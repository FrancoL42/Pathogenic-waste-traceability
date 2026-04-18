package ar.edu.utn.frc.tup.lciii.controllers;

import ar.edu.utn.frc.tup.lciii.dtos.report.sale.SalesReportFiltersDto;
import ar.edu.utn.frc.tup.lciii.dtos.report.sale.SalesReportResponseDto;
import ar.edu.utn.frc.tup.lciii.services.SalesReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reports/sales")
@RequiredArgsConstructor
@Tag(name = "Sales Reports", description = "Reportes de ventas de bolsas por generador")
@CrossOrigin(origins = "*")
public class SalesReportController {

    private final SalesReportService salesReportService;

    @Operation(
            summary = "Generar reporte de ventas de bolsas",
            description = "Genera un reporte detallado de ventas de bolsas por generador con filtros opcionales"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reporte generado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros de filtro inválidos"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/generate")
    public ResponseEntity<SalesReportResponseDto> generateSalesReport(
            @Valid @RequestBody SalesReportFiltersDto filters) {

        log.info("POST /api/reports/sales/generate - Filtros: {}", filters);

        try {
            SalesReportResponseDto response = salesReportService.generateSalesReport(filters);

            if (response.isSuccess()) {
                log.info("Reporte generado exitosamente con {} registros", response.getData().size());
                return ResponseEntity.ok(response);
            } else {
                log.warn("No se pudieron generar datos para el reporte: {}", response.getMessage());
                return ResponseEntity.ok(response); // Devolver respuesta con success=false pero 200 OK
            }

        } catch (Exception e) {
            log.error("Error al generar reporte de ventas", e);
            SalesReportResponseDto errorResponse = new SalesReportResponseDto(
                    false,
                    "Error interno al generar el reporte: " + e.getMessage(),
                    null,
                    null
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @Operation(
            summary = "Generar reporte con parámetros GET",
            description = "Genera reporte usando parámetros de consulta para mayor flexibilidad"
    )
    @GetMapping("/generate")
    public ResponseEntity<SalesReportResponseDto> generateSalesReportGet(
            @Parameter(description = "Fecha de inicio (formato: yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,

            @Parameter(description = "Fecha de fin (formato: yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,

            @Parameter(description = "Tipo de generador (Público/Privado)")
            @RequestParam(required = false) String tipoGenerador,

            @Parameter(description = "Nombre de la zona")
            @RequestParam(required = false) String zona) {

        log.info("GET /api/reports/sales/generate - Parámetros: fechaInicio={}, fechaFin={}, tipo={}, zona={}",
                fechaInicio, fechaFin, tipoGenerador, zona);

        SalesReportFiltersDto filters = new SalesReportFiltersDto(fechaInicio, fechaFin, tipoGenerador, zona);
        return generateSalesReport(filters);
    }

    @Operation(
            summary = "Obtener zonas disponibles",
            description = "Devuelve la lista de zonas disponibles para usar en los filtros"
    )
    @GetMapping("/zones")
    public ResponseEntity<List<String>> getAvailableZones() {
        log.info("GET /api/reports/sales/zones");

        try {
            List<String> zones = salesReportService.getAvailableZones();
            log.info("Zonas disponibles obtenidas: {}", zones.size());
            return ResponseEntity.ok(zones);

        } catch (Exception e) {
            log.error("Error al obtener zonas disponibles", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
            summary = "Exportar reporte a Excel",
            description = "Genera y descarga el reporte de ventas en formato Excel (.xlsx)"
    )
    @PostMapping("/export/excel")
    public ResponseEntity<ByteArrayResource> exportToExcel(
            @Valid @RequestBody SalesReportFiltersDto filters) {

        log.info("POST /api/reports/sales/export/excel - Filtros: {}", filters);

        try {
            ByteArrayResource resource = salesReportService.exportToExcel(filters);

            String filename = "reporte_ventas_bolsas_" +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (Exception e) {
            log.error("Error al exportar reporte a Excel", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
            summary = "Exportar reporte a Excel (GET)",
            description = "Descarga el reporte usando parámetros GET"
    )
    @GetMapping("/export/excel")
    public ResponseEntity<ByteArrayResource> exportToExcelGet(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,

            @RequestParam(required = false) String tipoGenerador,
            @RequestParam(required = false) String zona) {

        SalesReportFiltersDto filters = new SalesReportFiltersDto(fechaInicio, fechaFin, tipoGenerador, zona);
        return exportToExcel(filters);
    }

    @Operation(
            summary = "Exportar reporte a PDF",
            description = "Genera y descarga el reporte de ventas en formato PDF"
    )
    @PostMapping("/export/pdf")
    public ResponseEntity<ByteArrayResource> exportToPDF(
            @Valid @RequestBody SalesReportFiltersDto filters) {

        log.info("POST /api/reports/sales/export/pdf - Filtros: {}", filters);

        try {
            ByteArrayResource resource = salesReportService.exportToPDF(filters);

            String filename = "reporte_ventas_bolsas_" +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (Exception e) {
            log.error("Error al exportar reporte a PDF", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
            summary = "Exportar reporte a PDF (GET)",
            description = "Descarga el reporte en PDF usando parámetros GET"
    )
    @GetMapping("/export/pdf")
    public ResponseEntity<ByteArrayResource> exportToPDFGet(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,

            @RequestParam(required = false) String tipoGenerador,
            @RequestParam(required = false) String zona) {

        SalesReportFiltersDto filters = new SalesReportFiltersDto(fechaInicio, fechaFin, tipoGenerador, zona);
        return exportToPDF(filters);
    }

    @Operation(
            summary = "Health check del servicio de reportes",
            description = "Verifica que el servicio de reportes esté funcionando correctamente"
    )
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        log.info("GET /api/reports/sales/health - Health check");
        return ResponseEntity.ok("Sales Report Service is running");
    }
}
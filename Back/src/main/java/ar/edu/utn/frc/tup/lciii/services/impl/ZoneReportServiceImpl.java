package ar.edu.utn.frc.tup.lciii.services.impl;

import ar.edu.utn.frc.tup.lciii.dtos.ZoneReportDto;
import ar.edu.utn.frc.tup.lciii.dtos.report.zone.ZoneReportKpisDto;
import ar.edu.utn.frc.tup.lciii.dtos.report.zone.ZoneReportResponseDto;
import ar.edu.utn.frc.tup.lciii.entities.OrdersEntity;
import ar.edu.utn.frc.tup.lciii.entities.ZoneEntity;
import ar.edu.utn.frc.tup.lciii.repositories.OrdersRepository;
import ar.edu.utn.frc.tup.lciii.repositories.ZoneRepository;
import ar.edu.utn.frc.tup.lciii.services.ZoneReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
// Imports para Excel (Apache POI) - CON ALIAS ESPECÍFICOS
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// Imports para PDF (iText7) - CON NOMBRES COMPLETOS PARA EVITAR CONFLICTOS
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.colors.ColorConstants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZoneReportServiceImpl implements ZoneReportService {

    private final OrdersRepository ordersRepository;
    private final ZoneRepository zoneRepository;

    /**
     * Genera el reporte completo de zonas
     */
    @Override
    public ZoneReportResponseDto generateZoneReport(LocalDate fechaInicio, LocalDate fechaFin,
                                                    String estado, String zona) {
        try {
            List<ZoneReportDto> reportData = generateReportData(fechaInicio, fechaFin, estado, zona);
            ZoneReportKpisDto kpis = calculateKPIsFromData(reportData);

            return ZoneReportResponseDto.builder()
                    .success(true)
                    .data(reportData)
                    .kpis(kpis)
                    .message("Reporte generado exitosamente")
                    .build();
        } catch (Exception e) {
            log.error("Error al generar reporte de zonas", e);
            return ZoneReportResponseDto.builder()
                    .success(false)
                    .message("Error al generar el reporte: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Calcula solo los KPIs
     */
    @Override
    public ZoneReportKpisDto calculateKPIs(LocalDate fechaInicio, LocalDate fechaFin,
                                           String estado, String zona) {
        List<ZoneReportDto> reportData = generateReportData(fechaInicio, fechaFin, estado, zona);
        return calculateKPIsFromData(reportData);
    }

    /**
     * Genera los datos del reporte
     */
    private List<ZoneReportDto> generateReportData(LocalDate fechaInicio, LocalDate fechaFin,
                                                   String estado, String zona) {

        LocalDateTime startDateTime = fechaInicio != null ? fechaInicio.atStartOfDay() :
                LocalDateTime.now().minusDays(30);
        LocalDateTime endDateTime = fechaFin != null ? fechaFin.atTime(23, 59, 59) :
                LocalDateTime.now();

        List<OrdersEntity> orders = getFilteredOrders(startDateTime, endDateTime, estado, zona);

        // Agrupar por zona
        Map<String, List<OrdersEntity>> ordersByZone = orders.stream()
                .collect(Collectors.groupingBy(order -> order.getZoneEntity().getName()));

        List<ZoneReportDto> reportData = new ArrayList<>();

        for (Map.Entry<String, List<OrdersEntity>> entry : ordersByZone.entrySet()) {
            String zoneName = entry.getKey();
            List<OrdersEntity> zoneOrders = entry.getValue();

            ZoneReportDto zoneReport = generateZoneReport(zoneName, zoneOrders);
            reportData.add(zoneReport);
        }

        // Ordenar por total de solicitudes descendente
        return reportData.stream()
                .sorted((a, b) -> b.getTotalSolicitudes().compareTo(a.getTotalSolicitudes()))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene órdenes filtradas según los criterios
     */
    private List<OrdersEntity> getFilteredOrders(LocalDateTime startDateTime, LocalDateTime endDateTime,
                                                 String estado, String zona) {

        if (zona != null && !zona.equals("TODAS")) {
            if (estado != null && !estado.equals("TODOS")) {
                return ordersRepository.findByScheduledDateBetweenAndStateAndZoneEntity_Name(
                        startDateTime, endDateTime, estado, zona);
            } else {
                return ordersRepository.findByScheduledDateBetweenAndZoneEntity_Name(
                        startDateTime, endDateTime, zona);
            }
        } else {
            if (estado != null && !estado.equals("TODOS")) {
                return ordersRepository.findByScheduledDateBetweenAndState(
                        startDateTime, endDateTime, estado);
            } else {
                return ordersRepository.findByScheduledDateBetween(startDateTime, endDateTime);
            }
        }
    }

    /**
     * Genera el reporte para una zona específica
     */
    private ZoneReportDto generateZoneReport(String zoneName, List<OrdersEntity> orders) {
        int totalSolicitudes = orders.size();
        int pendientes = (int) orders.stream().filter(o -> "PENDIENTE".equals(o.getState())).count();
        int completadas = (int) orders.stream().filter(o -> "COMPLETADO".equals(o.getState())).count();
        int totalBolsas = orders.stream().mapToInt(o -> o.getCountBags() != null ? o.getCountBags() : 0).sum();

        // Fecha de última solicitud
        LocalDate fechaUltimaSolicitud = orders.stream()
                .map(o -> o.getScheduledDate().toLocalDate())
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());

        // Promedio de días de procesamiento
        double promedioDias = calculateAverageProcessingDays(orders);

        // Porcentaje completado
        double porcentajeCompletado = totalSolicitudes > 0 ?
                (double) completadas / totalSolicitudes * 100 : 0;

        return ZoneReportDto.builder()
                .zona(zoneName)
                .totalSolicitudes(totalSolicitudes)
                .solicitudesPendientes(pendientes)
                .solicitudesCompletadas(completadas)
                .totalBolsas(totalBolsas)
                .fechaUltimaSolicitud(fechaUltimaSolicitud)
                .promedioDiasProcesamiento(Math.round(promedioDias * 10.0) / 10.0)
                .porcentajeCompletado(Math.round(porcentajeCompletado * 10.0) / 10.0)
                .build();
    }

    /**
     * Calcula el promedio de días de procesamiento
     */
    private double calculateAverageProcessingDays(List<OrdersEntity> orders) {
        List<Long> processingDays = orders.stream()
                .filter(o -> "COMPLETADO".equals(o.getState()))
                .map(o -> {
                    LocalDateTime scheduled = o.getScheduledDate();
                    LocalDateTime completed = LocalDateTime.now(); // Aquí debería ser la fecha real de completado
                    return ChronoUnit.DAYS.between(scheduled, completed);
                })
                .collect(Collectors.toList());

        return processingDays.isEmpty() ? 0 :
                processingDays.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    /**
     * Calcula KPIs a partir de los datos del reporte
     */
    private ZoneReportKpisDto calculateKPIsFromData(List<ZoneReportDto> reportData) {
        if (reportData.isEmpty()) {
            return ZoneReportKpisDto.builder()
                    .totalSolicitudesGlobal(0)
                    .zonaConMasSolicitudes("N/A")
                    .promedioSolicitudesPorZona(0)
                    .porcentajeCompletadoGeneral(0.0)
                    .totalBolsasRecolectadas(0)
                    .tiempoPromedioRespuesta(0.0)
                    .build();
        }

        int totalSolicitudes = reportData.stream().mapToInt(ZoneReportDto::getTotalSolicitudes).sum();
        int totalCompletadas = reportData.stream().mapToInt(ZoneReportDto::getSolicitudesCompletadas).sum();
        int totalBolsas = reportData.stream().mapToInt(ZoneReportDto::getTotalBolsas).sum();

        String zonaConMasSolicitudes = reportData.stream()
                .max(Comparator.comparing(ZoneReportDto::getTotalSolicitudes))
                .map(ZoneReportDto::getZona)
                .orElse("N/A");

        double promedioTiempo = reportData.stream()
                .mapToDouble(ZoneReportDto::getPromedioDiasProcesamiento)
                .average()
                .orElse(0.0);

        double porcentajeCompletado = totalSolicitudes > 0 ?
                (double) totalCompletadas / totalSolicitudes * 100 : 0;

        return ZoneReportKpisDto.builder()
                .totalSolicitudesGlobal(totalSolicitudes)
                .zonaConMasSolicitudes(zonaConMasSolicitudes)
                .promedioSolicitudesPorZona(totalSolicitudes / reportData.size())
                .porcentajeCompletadoGeneral(Math.round(porcentajeCompletado * 10.0) / 10.0)
                .totalBolsasRecolectadas(totalBolsas)
                .tiempoPromedioRespuesta(Math.round(promedioTiempo * 10.0) / 10.0)
                .build();
    }

    /**
     * Obtiene detalles específicos de una zona
     */
    @Override
    public ZoneReportDto getZoneDetails(String zoneName, LocalDate fechaInicio, LocalDate fechaFin, String estado) {
        LocalDateTime startDateTime = fechaInicio != null ? fechaInicio.atStartOfDay() :
                LocalDateTime.now().minusDays(30);
        LocalDateTime endDateTime = fechaFin != null ? fechaFin.atTime(23, 59, 59) :
                LocalDateTime.now();

        List<OrdersEntity> orders = getFilteredOrders(startDateTime, endDateTime, estado, zoneName);
        return generateZoneReport(zoneName, orders);
    }

    /**
     * Obtiene lista de zonas disponibles
     */
    @Override
    public List<String> getAvailableZones() {
        return zoneRepository.findAll().stream()
                .map(ZoneEntity::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Exporta el reporte a Excel
     */
    @Override
    public byte[] exportToExcel(LocalDate fechaInicio, LocalDate fechaFin, String estado, String zona)
            throws IOException {

        List<ZoneReportDto> reportData = generateReportData(fechaInicio, fechaFin, estado, zona);
        ZoneReportKpisDto kpis = calculateKPIsFromData(reportData);

        try (Workbook workbook = new XSSFWorkbook()) {
            // Hoja de datos
            Sheet dataSheet = workbook.createSheet("Reporte Zonas");
            createDataSheet(dataSheet, reportData);

            // Hoja de KPIs
            Sheet kpisSheet = workbook.createSheet("KPIs");
            createKPIsSheet(kpisSheet, kpis);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Crea la hoja de datos en Excel
     */
    private void createDataSheet(Sheet sheet, List<ZoneReportDto> reportData) {
        // Crear header
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "Zona", "Total Solicitudes", "Solicitudes Pendientes", "Solicitudes Completadas",
                "Total Bolsas", "% Completado", "Última Solicitud", "Promedio Días Procesamiento"
        };

        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Crear filas de datos
        int rowNum = 1;
        for (ZoneReportDto data : reportData) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(data.getZona());
            row.createCell(1).setCellValue(data.getTotalSolicitudes());
            row.createCell(2).setCellValue(data.getSolicitudesPendientes());
            row.createCell(3).setCellValue(data.getSolicitudesCompletadas());
            row.createCell(4).setCellValue(data.getTotalBolsas());
            row.createCell(5).setCellValue(data.getPorcentajeCompletado() + "%");
            row.createCell(6).setCellValue(data.getFechaUltimaSolicitud().toString());
            row.createCell(7).setCellValue(data.getPromedioDiasProcesamiento());
        }

        // Ajustar ancho de columnas
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Crea la hoja de KPIs en Excel
     */
    private void createKPIsSheet(Sheet sheet, ZoneReportKpisDto kpis) {
        CellStyle titleStyle = sheet.getWorkbook().createCellStyle();
        Font titleFont = sheet.getWorkbook().createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        int rowNum = 0;

        // Título
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("KPIs del Reporte de Zonas");
        titleCell.setCellStyle(titleStyle);

        rowNum++; // Fila vacía

        // KPIs
        String[][] kpiData = {
                {"Total Solicitudes Global", kpis.getTotalSolicitudesGlobal().toString()},
                {"Zona con Más Solicitudes", kpis.getZonaConMasSolicitudes()},
                {"Promedio Solicitudes por Zona", kpis.getPromedioSolicitudesPorZona().toString()},
                {"% Completado General", kpis.getPorcentajeCompletadoGeneral() + "%"},
                {"Total Bolsas Recolectadas", kpis.getTotalBolsasRecolectadas().toString()},
                {"Tiempo Promedio Respuesta (días)", kpis.getTiempoPromedioRespuesta().toString()}
        };

        for (String[] kpi : kpiData) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(kpi[0]);
            row.createCell(1).setCellValue(kpi[1]);
        }

        // Ajustar ancho de columnas
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    @Override

    /**
     * Exporta el reporte a PDF usando iText7
     */
    public byte[] exportToPDF(LocalDate fechaInicio, LocalDate fechaFin, String estado, String zona) {
        try {
            List<ZoneReportDto> reportData = generateReportData(fechaInicio, fechaFin, estado, zona);
            ZoneReportKpisDto kpis = calculateKPIsFromData(reportData);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            // Título principal
            document.add(new Paragraph("REPORTE DE ZONAS")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(18)
                    .setMarginBottom(20));

            // Información del reporte
            String fechaReporte = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            document.add(new Paragraph("Fecha de generación: " + fechaReporte)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setFontSize(10)
                    .setMarginBottom(10));

            // Filtros aplicados
            if (fechaInicio != null || fechaFin != null || estado != null || zona != null) {
                document.add(new Paragraph("FILTROS APLICADOS")
                        .setBold()
                        .setFontSize(12)
                        .setMarginBottom(10));

                if (fechaInicio != null) {
                    document.add(new Paragraph("• Fecha inicio: " + fechaInicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                            .setFontSize(10)
                            .setMarginLeft(20));
                }
                if (fechaFin != null) {
                    document.add(new Paragraph("• Fecha fin: " + fechaFin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                            .setFontSize(10)
                            .setMarginLeft(20));
                }
                if (estado != null && !estado.isEmpty()) {
                    document.add(new Paragraph("• Estado: " + estado)
                            .setFontSize(10)
                            .setMarginLeft(20));
                }
                if (zona != null && !zona.isEmpty()) {
                    document.add(new Paragraph("• Zona: " + zona)
                            .setFontSize(10)
                            .setMarginLeft(20)
                            .setMarginBottom(20));
                }
            }

            // Sección de KPIs
            document.add(new Paragraph("INDICADORES CLAVE (KPIs)")
                    .setBold()
                    .setFontSize(14)
                    .setMarginBottom(15));

            // Tabla de KPIs - USANDO NOMBRES COMPLETOS PARA EVITAR CONFLICTOS
            Table kpisTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(20);

            // Headers de KPIs - USANDO com.itextpdf.layout.element.Cell EXPLÍCITAMENTE
            kpisTable.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Indicador").setBold())
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER));
            kpisTable.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Valor").setBold())
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER));

            // Datos de KPIs
            kpisTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Total Solicitudes Global")));
            kpisTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(kpis.getTotalSolicitudesGlobal().toString()))
                    .setTextAlignment(TextAlignment.CENTER));

            kpisTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Zona con Más Solicitudes")));
            kpisTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(kpis.getZonaConMasSolicitudes()))
                    .setTextAlignment(TextAlignment.CENTER));

            kpisTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Promedio Solicitudes por Zona")));
            kpisTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(kpis.getPromedioSolicitudesPorZona().toString()))
                    .setTextAlignment(TextAlignment.CENTER));

            kpisTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("% Completado General")));
            kpisTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.format("%.1f%%", kpis.getPorcentajeCompletadoGeneral())))
                    .setTextAlignment(TextAlignment.CENTER));

            kpisTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Total Bolsas Recolectadas")));
            kpisTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(kpis.getTotalBolsasRecolectadas().toString()))
                    .setTextAlignment(TextAlignment.CENTER));

            kpisTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Tiempo Promedio Respuesta (días)")));
            kpisTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(kpis.getTiempoPromedioRespuesta().toString()))
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(kpisTable);

            // Sección de datos detallados
            document.add(new Paragraph("DETALLE POR ZONA")
                    .setBold()
                    .setFontSize(14)
                    .setMarginBottom(15));

            // Tabla de datos
            Table dataTable = new Table(UnitValue.createPercentArray(new float[]{2, 1, 1, 1, 1, 1, 2, 1}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setFontSize(9);

            // Headers de la tabla
            String[] headers = {
                    "Zona", "Total Sol.", "Pendientes", "Completadas",
                    "Bolsas", "% Compl.", "Última Sol.", "Días Prom."
            };

            for (String header : headers) {
                dataTable.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(header).setBold())
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(8));
            }

            // Datos de la tabla
            for (ZoneReportDto data : reportData) {
                dataTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(data.getZona()))
                        .setFontSize(8));
                dataTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(data.getTotalSolicitudes().toString()))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(8));
                dataTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(data.getSolicitudesPendientes().toString()))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(8));
                dataTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(data.getSolicitudesCompletadas().toString()))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(8));
                dataTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(data.getTotalBolsas().toString()))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(8));
                dataTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.format("%.1f%%", data.getPorcentajeCompletado())))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(8));
                dataTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(data.getFechaUltimaSolicitud().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(8));
                dataTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(data.getPromedioDiasProcesamiento().toString()))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(8));
            }

            document.add(dataTable);

            // Pie de página
            document.add(new Paragraph("\n\nReporte generado automáticamente por el Sistema de Trazabilidad")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(8)
                    .setMarginTop(30));

            document.close();

            log.info("PDF generado exitosamente con {} zonas", reportData.size());
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Error al generar PDF del reporte de zonas", e);
            throw new RuntimeException("Error al generar el archivo PDF: " + e.getMessage(), e);
        }
    }
}
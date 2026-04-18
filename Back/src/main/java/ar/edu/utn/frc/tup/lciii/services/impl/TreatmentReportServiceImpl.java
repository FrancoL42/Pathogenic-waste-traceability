package ar.edu.utn.frc.tup.lciii.services.impl;

import ar.edu.utn.frc.tup.lciii.dtos.report.treatment.*;
import ar.edu.utn.frc.tup.lciii.repositories.TreatmentReportRepository;
import ar.edu.utn.frc.tup.lciii.services.TreatmentReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TreatmentReportServiceImpl implements TreatmentReportService {

    private final TreatmentReportRepository treatmentReportRepository;

    @Override
    public TreatmentReportResponseDto generateTreatmentReport(TreatmentReportFiltersDto filters) {
        try {
            log.info("Generando reporte de tratamientos con filtros: {}", filters);

            // Convertir fechas
            LocalDate fechaInicio = filters.getFechaInicio();
            LocalDate fechaFin = filters.getFechaFin();

            // Obtener datos de tratamientos
            List<Object[]> treatmentData = treatmentReportRepository.getTreatmentData(
                    fechaInicio, fechaFin, filters.getTipoGenerador(), filters.getZona()
            );

            if (treatmentData.isEmpty()) {
                log.warn("No se encontraron tratamientos para los filtros especificados: {}", filters);
                return new TreatmentReportResponseDto(false, "No se encontraron datos para los filtros especificados",
                        new ArrayList<>(), new TreatmentReportKPIsDto(0, 0, 0.0, 0.0, "", ""));
            }

            // Convertir a DTOs
            List<TreatmentReportDto> reportData = convertToTreatmentDtos(treatmentData);

            // Calcular KPIs
            TreatmentReportKPIsDto kpis = calculateTreatmentKPIs(filters, fechaInicio, fechaFin, reportData);

            log.info("Reporte de tratamientos generado exitosamente con {} registros", reportData.size());
            return new TreatmentReportResponseDto(true, "Reporte generado exitosamente", reportData, kpis);

        } catch (Exception e) {
            log.error("Error al generar reporte de tratamientos con filtros: " + filters, e);
            return new TreatmentReportResponseDto(false, "Error al generar el reporte: " + e.getMessage(),
                    new ArrayList<>(), new TreatmentReportKPIsDto(0, 0, 0.0, 0.0, "", ""));
        }
    }

    @Override
    public List<String> getAvailableZones() {
        try {
            List<String> zones = treatmentReportRepository.getAvailableZones();
            if (zones.isEmpty()) {
                log.info("No se encontraron zonas en la base de datos, devolviendo zonas por defecto");
                return Arrays.asList("Centro", "Nueva Córdoba", "Barrio Güemes", "Cerro de las Rosas", "Barrio General Paz");
            }
            return zones;
        } catch (Exception e) {
            log.error("Error al obtener zonas disponibles", e);
            return Arrays.asList("Centro", "Nueva Córdoba", "Barrio Güemes", "Cerro de las Rosas", "Barrio General Paz");
        }
    }

    @Override
    public ByteArrayResource exportToExcel(TreatmentReportFiltersDto filters) {
        try {
            TreatmentReportResponseDto reportData = generateTreatmentReport(filters);

            if (!reportData.isSuccess() || reportData.getData().isEmpty()) {
                throw new RuntimeException("No hay datos para exportar");
            }

            return createExcelFile(reportData.getData(), reportData.getKpis(), filters);

        } catch (Exception e) {
            log.error("Error al exportar a Excel", e);
            throw new RuntimeException("Error al generar el archivo Excel: " + e.getMessage());
        }
    }

    @Override
    public ByteArrayResource exportToPDF(TreatmentReportFiltersDto filters) {
        try {
            TreatmentReportResponseDto reportData = generateTreatmentReport(filters);

            if (!reportData.isSuccess() || reportData.getData().isEmpty()) {
                throw new RuntimeException("No hay datos para exportar");
            }

            byte[] pdfBytes = createPDFFile(reportData.getData(), reportData.getKpis(), filters);
            return new ByteArrayResource(pdfBytes);

        } catch (Exception e) {
            log.error("Error al exportar a PDF", e);
            throw new RuntimeException("Error al generar el archivo PDF: " + e.getMessage());
        }
    }

    /**
     * Convierte los datos agregados a DTOs del reporte
     */
    private List<TreatmentReportDto> convertToTreatmentDtos(List<Object[]> treatmentData) {
        return treatmentData.stream().map(row -> {
            LocalDate fechaTratamiento = (LocalDate) row[0];
            String generatorName = (String) row[1];
            String tipoGenerador = (String) row[2];
            String zona = (String) row[3];
            Long cantidadBolsas = (Long) row[4];
            Double pesoTotal = (Double) row[5];

            // Verificar valores nulos y asignar defaults
            cantidadBolsas = cantidadBolsas != null ? cantidadBolsas : 0L;
            pesoTotal = pesoTotal != null ? pesoTotal : 0.0;
            tipoGenerador = tipoGenerador != null ? tipoGenerador : "N/A";
            zona = zona != null ? zona : "Sin zona";

            return new TreatmentReportDto(
                    fechaTratamiento,
                    generatorName,
                    tipoGenerador,
                    zona,
                    cantidadBolsas.intValue(),
                    Math.round(pesoTotal * 100.0) / 100.0
            );
        }).collect(Collectors.toList());
    }

    /**
     * Calcula los KPIs del reporte de tratamientos
     */
    private TreatmentReportKPIsDto calculateTreatmentKPIs(TreatmentReportFiltersDto filters,
                                                          LocalDate fechaInicio,
                                                          LocalDate fechaFin,
                                                          List<TreatmentReportDto> reportData) {

        if (reportData.isEmpty()) {
            return new TreatmentReportKPIsDto(0, 0, 0.0, 0.0, "", "");
        }

        // Calcular totales
        int totalTratamientos = reportData.size();
        int totalBolsas = reportData.stream().mapToInt(TreatmentReportDto::getCantidadBolsas).sum();
        double totalPeso = reportData.stream().mapToDouble(TreatmentReportDto::getPesoTotal).sum();

        // Promedio de peso por bolsa
        double promedioPesoPorBolsa = totalBolsas > 0 ? totalPeso / totalBolsas : 0.0;

        // Generador con más tratamientos
        String generadorTop = reportData.stream()
                .collect(Collectors.groupingBy(TreatmentReportDto::getGenerador,
                        Collectors.summingInt(TreatmentReportDto::getCantidadBolsas)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");

        // Día con más tratamientos
        String diaMasTratamientos = reportData.stream()
                .collect(Collectors.groupingBy(TreatmentReportDto::getFechaTratamiento,
                        Collectors.summingInt(TreatmentReportDto::getCantidadBolsas)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .orElse("");

        return new TreatmentReportKPIsDto(
                totalTratamientos,
                totalBolsas,
                Math.round(totalPeso * 100.0) / 100.0,
                Math.round(promedioPesoPorBolsa * 100.0) / 100.0,
                generadorTop,
                diaMasTratamientos
        );
    }

    /**
     * Crea el archivo Excel con los datos del reporte
     */
    private ByteArrayResource createExcelFile(List<TreatmentReportDto> data,
                                              TreatmentReportKPIsDto kpis,
                                              TreatmentReportFiltersDto filters) throws IOException {

        try (Workbook workbook = new XSSFWorkbook()) {
            // Hoja de datos principales
            Sheet dataSheet = workbook.createSheet("Reporte Tratamientos");

            // Crear estilos
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yyyy"));

            // Headers
            Row headerRow = dataSheet.createRow(0);
            String[] headers = {
                    "Fecha Tratamiento", "Generador", "Tipo", "Zona",
                    "Cantidad Bolsas", "Peso Total (kg)"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Datos
            int rowNum = 1;
            for (TreatmentReportDto item : data) {
                Row row = dataSheet.createRow(rowNum++);

                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(item.getFechaTratamiento().toString());
                dateCell.setCellStyle(dateStyle);

                row.createCell(1).setCellValue(item.getGenerador());
                row.createCell(2).setCellValue(item.getTipoGenerador());
                row.createCell(3).setCellValue(item.getZona());
                row.createCell(4).setCellValue(item.getCantidadBolsas());
                row.createCell(5).setCellValue(item.getPesoTotal());
            }

            // Auto-ajustar columnas
            for (int i = 0; i < headers.length; i++) {
                dataSheet.autoSizeColumn(i);
            }

            // Hoja de KPIs
            createTreatmentKPIsSheet(workbook, kpis, filters);

            // Convertir a ByteArrayResource
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);

            return new ByteArrayResource(outputStream.toByteArray());
        }
    }

    /**
     * Crea la hoja de KPIs en el Excel
     */
    private void createTreatmentKPIsSheet(Workbook workbook, TreatmentReportKPIsDto kpis, TreatmentReportFiltersDto filters) {
        Sheet kpisSheet = workbook.createSheet("KPIs y Resumen");

        // Estilo para títulos
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        int rowNum = 0;

        // Título
        Row titleRow = kpisSheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("REPORTE DE TRATAMIENTOS REALIZADOS - RESUMEN EJECUTIVO");
        titleCell.setCellStyle(titleStyle);

        rowNum++; // Línea en blanco

        // Información de filtros
        if (filters.getFechaInicio() != null || filters.getFechaFin() != null) {
            Row filtersRow = kpisSheet.createRow(rowNum++);
            String dateRange = "Período: ";
            if (filters.getFechaInicio() != null) {
                dateRange += filters.getFechaInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            dateRange += " - ";
            if (filters.getFechaFin() != null) {
                dateRange += filters.getFechaFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            filtersRow.createCell(0).setCellValue(dateRange);
        }

        if (filters.getTipoGenerador() != null) {
            Row typeRow = kpisSheet.createRow(rowNum++);
            typeRow.createCell(0).setCellValue("Tipo de Generador: " + filters.getTipoGenerador());
        }

        if (filters.getZona() != null) {
            Row zoneRow = kpisSheet.createRow(rowNum++);
            zoneRow.createCell(0).setCellValue("Zona: " + filters.getZona());
        }

        rowNum++; // Línea en blanco

        // KPIs
        Row kpiTitleRow = kpisSheet.createRow(rowNum++);
        kpiTitleRow.createCell(0).setCellValue("INDICADORES CLAVE (KPIs)");
        kpiTitleRow.getCell(0).setCellStyle(titleStyle);

        kpisSheet.createRow(rowNum++).createCell(0).setCellValue("Total Tratamientos: " + kpis.getTotalTratamientos());
        kpisSheet.createRow(rowNum++).createCell(0).setCellValue("Total Bolsas Tratadas: " + kpis.getTotalBolsas());
        kpisSheet.createRow(rowNum++).createCell(0).setCellValue("Peso Total Tratado: " + kpis.getPesoTotal() + " kg");
        kpisSheet.createRow(rowNum++).createCell(0).setCellValue("Promedio Peso por Bolsa: " + kpis.getPromedioPesoPorBolsa() + " kg");
        kpisSheet.createRow(rowNum++).createCell(0).setCellValue("Generador Top: " + kpis.getGeneradorTop());
        kpisSheet.createRow(rowNum++).createCell(0).setCellValue("Día con Más Tratamientos: " + kpis.getDiaMasTratamientos());

        // Auto-ajustar columnas
        kpisSheet.autoSizeColumn(0);
        kpisSheet.autoSizeColumn(1);
    }

    /**
     * Crea el archivo PDF con los datos del reporte
     */
    private byte[] createPDFFile(List<TreatmentReportDto> data,
                                 TreatmentReportKPIsDto kpis,
                                 TreatmentReportFiltersDto filters) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(outputStream);
            com.itextpdf.kernel.pdf.PdfDocument pdfDocument = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdfDocument);

            // Título principal
            document.add(new com.itextpdf.layout.element.Paragraph("REPORTE DE TRATAMIENTOS REALIZADOS")
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(16)
                    .setMarginBottom(20));

            // Información del reporte
            String fechaReporte = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            document.add(new com.itextpdf.layout.element.Paragraph("Fecha de generación: " + fechaReporte)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)
                    .setFontSize(10)
                    .setMarginBottom(10));

            // Filtros aplicados
            if (filters.getFechaInicio() != null || filters.getFechaFin() != null ||
                    filters.getTipoGenerador() != null || filters.getZona() != null) {

                document.add(new com.itextpdf.layout.element.Paragraph("FILTROS APLICADOS")
                        .setBold()
                        .setFontSize(12)
                        .setMarginBottom(10));

                if (filters.getFechaInicio() != null) {
                    document.add(new com.itextpdf.layout.element.Paragraph("• Fecha inicio: " +
                            filters.getFechaInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                            .setFontSize(10)
                            .setMarginLeft(20));
                }
                if (filters.getFechaFin() != null) {
                    document.add(new com.itextpdf.layout.element.Paragraph("• Fecha fin: " +
                            filters.getFechaFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                            .setFontSize(10)
                            .setMarginLeft(20));
                }
                if (filters.getTipoGenerador() != null && !filters.getTipoGenerador().isEmpty()) {
                    document.add(new com.itextpdf.layout.element.Paragraph("• Tipo de Generador: " + filters.getTipoGenerador())
                            .setFontSize(10)
                            .setMarginLeft(20));
                }
                if (filters.getZona() != null && !filters.getZona().isEmpty()) {
                    document.add(new com.itextpdf.layout.element.Paragraph("• Zona: " + filters.getZona())
                            .setFontSize(10)
                            .setMarginLeft(20)
                            .setMarginBottom(20));
                }
            }

            // Sección de KPIs
            document.add(new com.itextpdf.layout.element.Paragraph("INDICADORES CLAVE (KPIs)")
                    .setBold()
                    .setFontSize(14)
                    .setMarginBottom(15));

            // Tabla de KPIs
            com.itextpdf.layout.element.Table kpisTable = new com.itextpdf.layout.element.Table(
                    com.itextpdf.layout.properties.UnitValue.createPercentArray(new float[]{3, 1}))
                    .setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100))
                    .setMarginBottom(20);

            // Headers de KPIs
            kpisTable.addHeaderCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph("Indicador").setBold())
                    .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
            kpisTable.addHeaderCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph("Valor").setBold())
                    .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            // Datos de KPIs
            kpisTable.addCell("Total Tratamientos");
            kpisTable.addCell(String.valueOf(kpis.getTotalTratamientos()));

            kpisTable.addCell("Total Bolsas Tratadas");
            kpisTable.addCell(String.valueOf(kpis.getTotalBolsas()));

            kpisTable.addCell("Peso Total Tratado");
            kpisTable.addCell(String.format("%.2f kg", kpis.getPesoTotal()));

            kpisTable.addCell("Promedio Peso por Bolsa");
            kpisTable.addCell(String.format("%.2f kg", kpis.getPromedioPesoPorBolsa()));

            kpisTable.addCell("Generador Top");
            kpisTable.addCell(kpis.getGeneradorTop());

            kpisTable.addCell("Día con Más Tratamientos");
            kpisTable.addCell(kpis.getDiaMasTratamientos());

            document.add(kpisTable);

            // Sección de datos detallados
            document.add(new com.itextpdf.layout.element.Paragraph("DETALLE DE TRATAMIENTOS")
                    .setBold()
                    .setFontSize(14)
                    .setMarginBottom(15));

            // Tabla de datos
            com.itextpdf.layout.element.Table dataTable = new com.itextpdf.layout.element.Table(
                    com.itextpdf.layout.properties.UnitValue.createPercentArray(new float[]{2, 2, 1, 2, 1, 1.5f}))
                    .setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100))
                    .setFontSize(8);

            // Headers de la tabla
            String[] headers = {"Fecha", "Generador", "Tipo", "Zona", "Bolsas", "Peso (kg)"};

            for (String header : headers) {
                dataTable.addHeaderCell(new com.itextpdf.layout.element.Cell()
                        .add(new com.itextpdf.layout.element.Paragraph(header).setBold())
                        .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY)
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                        .setFontSize(8));
            }

            // Datos de la tabla
            for (TreatmentReportDto treatmentData : data) {
                dataTable.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new com.itextpdf.layout.element.Paragraph(
                                treatmentData.getFechaTratamiento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
                        .setFontSize(7)
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

                dataTable.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new com.itextpdf.layout.element.Paragraph(treatmentData.getGenerador()))
                        .setFontSize(7));

                dataTable.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new com.itextpdf.layout.element.Paragraph(treatmentData.getTipoGenerador()))
                        .setFontSize(7)
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

                dataTable.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new com.itextpdf.layout.element.Paragraph(treatmentData.getZona()))
                        .setFontSize(7)
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

                dataTable.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new com.itextpdf.layout.element.Paragraph(String.valueOf(treatmentData.getCantidadBolsas())))
                        .setFontSize(7)
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

                dataTable.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new com.itextpdf.layout.element.Paragraph(String.format("%.2f", treatmentData.getPesoTotal())))
                        .setFontSize(7)
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
            }

            document.add(dataTable);

            // Pie de página
            document.add(new com.itextpdf.layout.element.Paragraph("\n\nReporte generado automáticamente por el Sistema de Gestión de Residuos Patológicos")
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setFontSize(8)
                    .setMarginTop(30));

            document.close();

            log.info("PDF de tratamientos generado exitosamente con {} registros", data.size());
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Error al generar PDF del reporte de tratamientos", e);
            throw new RuntimeException("Error al generar el archivo PDF: " + e.getMessage(), e);
        }
    }
}
package ar.edu.utn.frc.tup.lciii.services.impl;

import ar.edu.utn.frc.tup.lciii.dtos.*;
import ar.edu.utn.frc.tup.lciii.dtos.report.sale.SalesReportDto;
import ar.edu.utn.frc.tup.lciii.dtos.report.sale.SalesReportFiltersDto;
import ar.edu.utn.frc.tup.lciii.dtos.report.sale.SalesReportKPIsDto;
import ar.edu.utn.frc.tup.lciii.dtos.report.sale.SalesReportResponseDto;
import ar.edu.utn.frc.tup.lciii.entities.SalesEntity;
import ar.edu.utn.frc.tup.lciii.repositories.SalesReportRepository;
import ar.edu.utn.frc.tup.lciii.services.SalesReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesReportServiceImpl implements SalesReportService {

    private final SalesReportRepository salesReportRepository;

    @Override
    public SalesReportResponseDto generateSalesReport(SalesReportFiltersDto filters) {
        try {
            log.info("Generando reporte de ventas con filtros: {}", filters);

            // Convertir fechas a LocalDateTime
            LocalDateTime fechaInicio = filters.getFechaInicio() != null ?
                    filters.getFechaInicio().atStartOfDay() : null;
            LocalDateTime fechaFin = filters.getFechaFin() != null ?
                    filters.getFechaFin().atTime(23, 59, 59) : null;

            // Obtener datos agregados por generador
            List<Object[]> aggregatedData = salesReportRepository.getSalesAggregatedByGenerator(
                    fechaInicio, fechaFin, filters.getTipoGenerador(), filters.getZona()
            );

            if (aggregatedData.isEmpty()) {
                log.warn("No se encontraron datos para los filtros especificados: {}", filters);
                return new SalesReportResponseDto(false, "No se encontraron datos para los filtros especificados",
                        new ArrayList<>(), new SalesReportKPIsDto(0, "", 0, 0.0, 0.0, 0.0, 0.0));
            }

            // Convertir a DTOs
            List<SalesReportDto> reportData = convertToReportDtos(aggregatedData, fechaInicio, fechaFin);

            // Calcular KPIs
            SalesReportKPIsDto kpis = calculateKPIs(filters, fechaInicio, fechaFin, reportData);

            log.info("Reporte generado exitosamente con {} registros", reportData.size());
            return new SalesReportResponseDto(true, "Reporte generado exitosamente", reportData, kpis);

        } catch (Exception e) {
            log.error("Error al generar reporte de ventas con filtros: " + filters, e);
            return new SalesReportResponseDto(false, "Error al generar el reporte: " + e.getMessage(),
                    new ArrayList<>(), new SalesReportKPIsDto(0, "", 0, 0.0, 0.0, 0.0, 0.0));
        }
    }

    @Override
    public List<String> getAvailableZones() {
        try {
            List<String> zones = salesReportRepository.getAvailableZones();

            // Si no hay zonas en la BD, devolver zonas por defecto
            if (zones.isEmpty()) {
                log.info("No se encontraron zonas en la base de datos, devolviendo zonas por defecto");
                return Arrays.asList("Zona Centro", "Zona Norte", "Zona Sur", "Zona Este", "Zona Oeste");
            }

            return zones;
        } catch (Exception e) {
            log.error("Error al obtener zonas disponibles", e);
            return Arrays.asList("Zona Centro", "Zona Norte", "Zona Sur", "Zona Este", "Zona Oeste");
        }
    }

    @Override
    public ByteArrayResource exportToExcel(SalesReportFiltersDto filters) {
        try {
            SalesReportResponseDto reportData = generateSalesReport(filters);

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
    public ByteArrayResource exportToPDF(SalesReportFiltersDto filters) {
        try {
            SalesReportResponseDto reportData = generateSalesReport(filters);

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
    private List<SalesReportDto> convertToReportDtos(List<Object[]> aggregatedData,
                                                     LocalDateTime fechaInicio,
                                                     LocalDateTime fechaFin) {

        // Obtener datos de crecimiento mensual para el cálculo
        Map<String, Double> growthData = calculateMonthlyGrowth(fechaInicio, fechaFin);

        return aggregatedData.stream().map(row -> {
            String generatorName = (String) row[0];
            Long totalSales = (Long) row[1];
            Long totalQuantity = (Long) row[2];
            Double totalAmount = (Double) row[3];
            LocalDateTime lastSaleDate = (LocalDateTime) row[4];

            // Verificar valores nulos y asignar defaults
            totalSales = totalSales != null ? totalSales : 0L;
            totalQuantity = totalQuantity != null ? totalQuantity : 0L;
            totalAmount = totalAmount != null ? totalAmount : 0.0;

            // Buscar información adicional del generador
            SalesEntity sampleSale = null;
            try {
                List<SalesEntity> sales = salesReportRepository.findSalesForReport(
                        fechaInicio, fechaFin, null, null
                );
                sampleSale = sales.stream()
                        .filter(s -> s.getGeneratorEntity().getName().equals(generatorName))
                        .findFirst()
                        .orElse(null);
            } catch (Exception e) {
                log.warn("Error al buscar información del generador {}: {}", generatorName, e.getMessage());
            }

            String tipoGenerador = "N/A";
            String zona = "Sin zona";

            if (sampleSale != null) {
                tipoGenerador = sampleSale.getGeneratorEntity().getType() != null ?
                        sampleSale.getGeneratorEntity().getType() : "N/A";
                zona = sampleSale.getGeneratorEntity().getZone() != null &&
                        sampleSale.getGeneratorEntity().getZone().getName() != null ?
                        sampleSale.getGeneratorEntity().getZone().getName() : "Sin zona";
            }

            // Calcular promedio mensual
            long daysBetween = fechaInicio != null && fechaFin != null ?
                    java.time.temporal.ChronoUnit.DAYS.between(fechaInicio, fechaFin) : 90;
            double monthsInPeriod = Math.max(1, daysBetween / 30.0);
            double averagePerMonth = totalSales.doubleValue() / monthsInPeriod;

            // Obtener crecimiento mensual
            double monthlyGrowth = growthData.getOrDefault(generatorName, 0.0);

            return new SalesReportDto(
                    generatorName,
                    tipoGenerador,
                    zona,
                    totalSales.intValue(),
                    totalQuantity.intValue(),
                    totalAmount,
                    lastSaleDate,
                    Math.round(averagePerMonth * 100.0) / 100.0,
                    Math.round(monthlyGrowth * 100.0) / 100.0
            );
        }).collect(Collectors.toList());
    }

    /**
     * Calcula el crecimiento mensual por generador
     */
    private Map<String, Double> calculateMonthlyGrowth(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            return new HashMap<>();
        }

        List<Object[]> monthlyData = salesReportRepository.getMonthlyDataForGrowthCalculation(fechaInicio, fechaFin);
        Map<String, List<Integer>> generatorMonthlyQuantities = new HashMap<>();

        // Agrupar datos por generador
        for (Object[] row : monthlyData) {
            String generatorName = (String) row[0];
            Long quantity = (Long) row[3];

            // Verificar que quantity no sea null
            if (quantity != null) {
                generatorMonthlyQuantities.computeIfAbsent(generatorName, k -> new ArrayList<>())
                        .add(quantity.intValue());
            }
        }

        // Calcular crecimiento para cada generador
        Map<String, Double> growthData = new HashMap<>();
        for (Map.Entry<String, List<Integer>> entry : generatorMonthlyQuantities.entrySet()) {
            List<Integer> quantities = entry.getValue();
            if (quantities.size() >= 2) {
                int firstMonth = quantities.get(0);
                int lastMonth = quantities.get(quantities.size() - 1);

                if (firstMonth > 0) {
                    double growth = ((double) (lastMonth - firstMonth) / firstMonth) * 100;
                    growthData.put(entry.getKey(), growth);
                } else {
                    // Si el primer mes es 0, calcular como crecimiento desde 0
                    growthData.put(entry.getKey(), lastMonth > 0 ? 100.0 : 0.0);
                }
            } else if (quantities.size() == 1) {
                // Solo un mes de datos, no hay crecimiento calculable
                growthData.put(entry.getKey(), 0.0);
            }
        }

        return growthData;
    }

    /**
     * Calcula los KPIs del reporte
     */
    private SalesReportKPIsDto calculateKPIs(SalesReportFiltersDto filters,
                                             LocalDateTime fechaInicio,
                                             LocalDateTime fechaFin,
                                             List<SalesReportDto> reportData) {

        if (reportData.isEmpty()) {
            return new SalesReportKPIsDto(0, "", 0, 0.0, 0.0, 0.0, 0.0);
        }

        // Obtener estadísticas por tipo con validación de nulos
        List<Object[]> typeStats = salesReportRepository.getSalesStatsByType(
                fechaInicio, fechaFin, filters.getTipoGenerador(), filters.getZona()
        );

        // Calcular totales desde reportData (ya validados)
        int totalVentas = reportData.stream().mapToInt(SalesReportDto::getTotalVentas).sum();
        int totalBolsas = reportData.stream().mapToInt(SalesReportDto::getCantidadBolsas).sum();
        double totalMonto = reportData.stream().mapToDouble(SalesReportDto::getMontoTotal).sum();

        // Promedio de bolsas por período
        int promedioBolsas = reportData.isEmpty() ? 0 : totalBolsas / reportData.size();

        // Generador con más ventas
        String topGenerator = reportData.stream()
                .max(Comparator.comparing(SalesReportDto::getCantidadBolsas))
                .map(SalesReportDto::getGenerador)
                .orElse("");

        // Porcentajes por tipo con validación de nulos
        double porcentajePublico = 0.0;
        double porcentajePrivado = 0.0;

        for (Object[] typeRow : typeStats) {
            try {
                String type = (String) typeRow[0];
                Long quantity = (Long) typeRow[2];

                // Validar que quantity no sea null y totalBolsas > 0
                if (quantity != null && totalBolsas > 0) {
                    double percentage = (quantity.doubleValue() / totalBolsas) * 100;
                    if ("Público".equalsIgnoreCase(type)) {
                        porcentajePublico = Math.round(percentage * 10.0) / 10.0;
                    } else if ("Privado".equalsIgnoreCase(type)) {
                        porcentajePrivado = Math.round(percentage * 10.0) / 10.0;
                    }
                }
            } catch (Exception e) {
                log.warn("Error al procesar estadísticas por tipo: {}", e.getMessage());
            }
        }

        // Tendencia de crecimiento promedio
        double tendenciaCrecimiento = reportData.stream()
                .mapToDouble(SalesReportDto::getCrecimientoMensual)
                .average()
                .orElse(0.0);

        return new SalesReportKPIsDto(
                totalVentas,
                topGenerator,
                promedioBolsas,
                porcentajePublico,
                porcentajePrivado,
                Math.round(tendenciaCrecimiento * 10.0) / 10.0,
                Math.round(totalMonto * 100.0) / 100.0
        );
    }

    /**
     * Crea el archivo Excel con los datos del reporte
     */
    private ByteArrayResource createExcelFile(List<SalesReportDto> data,
                                              SalesReportKPIsDto kpis,
                                              SalesReportFiltersDto filters) throws IOException {

        try (Workbook workbook = new XSSFWorkbook()) {
            // Hoja de datos principales
            Sheet dataSheet = workbook.createSheet("Reporte Ventas Bolsas");

            // Crear estilos
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yyyy"));

            // Headers
            Row headerRow = dataSheet.createRow(0);
            String[] headers = {
                    "Generador", "Tipo", "Zona", "Total Ventas", "Cantidad Bolsas",
                    "Monto Total", "Prom. Ventas/Mes", "Crecimiento %", "Última Venta"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Datos
            int rowNum = 1;
            for (SalesReportDto item : data) {
                Row row = dataSheet.createRow(rowNum++);

                row.createCell(0).setCellValue(item.getGenerador());
                row.createCell(1).setCellValue(item.getTipoGenerador());
                row.createCell(2).setCellValue(item.getZona());
                row.createCell(3).setCellValue(item.getTotalVentas());
                row.createCell(4).setCellValue(item.getCantidadBolsas());
                row.createCell(5).setCellValue(item.getMontoTotal());
                row.createCell(6).setCellValue(item.getPromedioVentasPorMes());
                row.createCell(7).setCellValue(item.getCrecimientoMensual());

                Cell dateCell = row.createCell(8);
                if (item.getFechaUltimaVenta() != null) {
                    dateCell.setCellValue(item.getFechaUltimaVenta().toLocalDate().toString());
                }
            }

            // Auto-ajustar columnas
            for (int i = 0; i < headers.length; i++) {
                dataSheet.autoSizeColumn(i);
            }

            // Hoja de KPIs
            createKPIsSheet(workbook, kpis, filters);

            // Convertir a ByteArrayResource
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);

            return new ByteArrayResource(outputStream.toByteArray());
        }
    }

    /**
     * Crea la hoja de KPIs en el Excel
     */
    private void createKPIsSheet(Workbook workbook, SalesReportKPIsDto kpis, SalesReportFiltersDto filters) {
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
        titleCell.setCellValue("REPORTE DE VENTAS DE BOLSAS - RESUMEN EJECUTIVO");
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

        kpisSheet.createRow(rowNum++).createCell(0).setCellValue("Total Transacciones: " + kpis.getTotalVentasGlobal());
        kpisSheet.createRow(rowNum++).createCell(0).setCellValue("Promedio Bolsas por Período: " + kpis.getPromedioBolsasPorPeriodo());
        kpisSheet.createRow(rowNum++).createCell(0).setCellValue("Total Transacciones: " + kpis.getTotalVentasGlobal());
        kpisSheet.createRow(rowNum++).createCell(0).setCellValue("Promedio Bolsas por Período: " + kpis.getPromedioBolsasPorPeriodo());
        kpisSheet.createRow(rowNum++).createCell(0).setCellValue("Generador Top: " + kpis.getGeneradorConMasVentas());
        kpisSheet.createRow(rowNum++).createCell(0).setCellValue("% Generadores Públicos: " + kpis.getPorcentajeTipoPublico() + "%");
        kpisSheet.createRow(rowNum++).createCell(0).setCellValue("% Generadores Privados: " + kpis.getPorcentajeTipoPrivado() + "%");
        kpisSheet.createRow(rowNum++).createCell(0).setCellValue("Tendencia Crecimiento: " + kpis.getTendenciaCrecimiento() + "%");
        kpisSheet.createRow(rowNum++).createCell(0).setCellValue("Monto Total Ventas: $" + String.format("%.2f", kpis.getMontoTotalVentas()));

        // Auto-ajustar columnas
        kpisSheet.autoSizeColumn(0);
        kpisSheet.autoSizeColumn(1);
    }

    /**
     * Crea el archivo PDF con los datos del reporte
     */
    private byte[] createPDFFile(List<SalesReportDto> data,
                                 SalesReportKPIsDto kpis,
                                 SalesReportFiltersDto filters) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(outputStream);
            com.itextpdf.kernel.pdf.PdfDocument pdfDocument = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdfDocument);

            // Título principal
            document.add(new com.itextpdf.layout.element.Paragraph("REPORTE DE VENTAS DE BOLSAS POR GENERADOR")
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
            kpisTable.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph("Total Transacciones")));
            kpisTable.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph(kpis.getTotalVentasGlobal().toString()))
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            kpisTable.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph("Generador Top")));
            kpisTable.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph(kpis.getGeneradorConMasVentas()))
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            kpisTable.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph("Promedio Bolsas por Período")));
            kpisTable.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph(kpis.getPromedioBolsasPorPeriodo().toString()))
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            kpisTable.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph("% Generadores Públicos")));
            kpisTable.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph(String.format("%.1f%%", kpis.getPorcentajeTipoPublico())))
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            kpisTable.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph("% Generadores Privados")));
            kpisTable.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph(String.format("%.1f%%", kpis.getPorcentajeTipoPrivado())))
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            kpisTable.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph("Tendencia Crecimiento")));
            kpisTable.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph(String.format("%.1f%%", kpis.getTendenciaCrecimiento())))
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            kpisTable.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph("Monto Total Ventas")));
            kpisTable.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph(String.format("$%.2f", kpis.getMontoTotalVentas())))
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            document.add(kpisTable);

            // Sección de datos detallados
            document.add(new com.itextpdf.layout.element.Paragraph("DETALLE DE VENTAS POR GENERADOR")
                    .setBold()
                    .setFontSize(14)
                    .setMarginBottom(15));

            // Tabla de datos
            com.itextpdf.layout.element.Table dataTable = new com.itextpdf.layout.element.Table(
                    com.itextpdf.layout.properties.UnitValue.createPercentArray(new float[]{2, 1, 1, 1, 1.5f, 1, 1, 1.5f}))
                    .setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100))
                    .setFontSize(8);

            // Headers de la tabla
            String[] headers = {
                    "Generador", "Tipo", "Zona", "Ventas", "Bolsas",
                    "Monto", "Prom./Mes", "Última Venta"
            };

            for (String header : headers) {
                dataTable.addHeaderCell(new com.itextpdf.layout.element.Cell()
                        .add(new com.itextpdf.layout.element.Paragraph(header).setBold())
                        .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY)
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                        .setFontSize(8));
            }

            // Datos de la tabla
            for (SalesReportDto salesData : data) {
                dataTable.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new com.itextpdf.layout.element.Paragraph(salesData.getGenerador()))
                        .setFontSize(7));
                dataTable.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new com.itextpdf.layout.element.Paragraph(salesData.getTipoGenerador()))
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                        .setFontSize(7));
                dataTable.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new com.itextpdf.layout.element.Paragraph(salesData.getZona()))
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                        .setFontSize(7));
                dataTable.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new com.itextpdf.layout.element.Paragraph(salesData.getTotalVentas().toString()))
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                        .setFontSize(7));
                dataTable.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new com.itextpdf.layout.element.Paragraph(salesData.getCantidadBolsas().toString()))
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                        .setFontSize(7));
                dataTable.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new com.itextpdf.layout.element.Paragraph(String.format("$%.0f", salesData.getMontoTotal())))
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                        .setFontSize(7));
                dataTable.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new com.itextpdf.layout.element.Paragraph(String.format("%.1f", salesData.getPromedioVentasPorMes())))
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                        .setFontSize(7));

                // Formatear fecha
                String fechaFormateada = "N/A";
                if (salesData.getFechaUltimaVenta() != null) {
                    LocalDateTime fecha = salesData.getFechaUltimaVenta();
                    fechaFormateada = fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                }

                dataTable.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new com.itextpdf.layout.element.Paragraph(fechaFormateada))
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                        .setFontSize(7));
            }

            document.add(dataTable);

            // Pie de página
            document.add(new com.itextpdf.layout.element.Paragraph("\n\nReporte generado automáticamente por el Sistema de Gestión de Residuos Patológicos")
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setFontSize(8)
                    .setMarginTop(30));

            document.close();

            log.info("PDF de ventas generado exitosamente con {} generadores", data.size());
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Error al generar PDF del reporte de ventas", e);
            throw new RuntimeException("Error al generar el archivo PDF: " + e.getMessage(), e);
        }
    }
}
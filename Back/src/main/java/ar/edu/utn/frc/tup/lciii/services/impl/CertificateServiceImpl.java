package ar.edu.utn.frc.tup.lciii.services.impl;

import ar.edu.utn.frc.tup.lciii.entities.GeneratorEntity;
import ar.edu.utn.frc.tup.lciii.entities.SealEntity;
import ar.edu.utn.frc.tup.lciii.models.SealStatus;
import ar.edu.utn.frc.tup.lciii.repositories.SealRepository;
import ar.edu.utn.frc.tup.lciii.services.CertificateService;
import ar.edu.utn.frc.tup.lciii.services.EnvioCorreosService;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class CertificateServiceImpl implements CertificateService {

    @Autowired
    private SealRepository sealRepository;

    @Autowired
    private EnvioCorreosService envioCorreosService;

    @Override
    public void generarCertificadosDelDia(LocalDate fecha) {
        log.info("Iniciando generación de certificados para la fecha: {}", fecha);

        try {
            // Buscar todos los precintos tratados en la fecha
            List<SealEntity> precintosDelDia = sealRepository.findByStateAndFechaTratamiento(
                    SealStatus.TRATADO, fecha);

            if (precintosDelDia.isEmpty()) {
                log.info("No hay precintos tratados para la fecha: {}", fecha);
                return;
            }

            log.info("Encontrados {} precintos tratados para procesar", precintosDelDia.size());

            // Agrupar por generador
            Map<GeneratorEntity, List<SealEntity>> precintosGroup = precintosDelDia.stream()
                    .filter(seal -> seal.getGeneratorEntity() != null)
                    .collect(Collectors.groupingBy(SealEntity::getGeneratorEntity));

            log.info("Generando certificados para {} generadores", precintosGroup.size());

            // Generar y enviar certificado por cada generador
            precintosGroup.forEach((generador, precintos) -> {
                try {
                    enviarCertificadoPorEmail(generador, precintos, fecha);
                    log.info("Certificado generado y enviado para generador: {} - {} precintos",
                            generador.getName(), precintos.size());
                } catch (Exception e) {
                    log.error("Error generando/enviando certificado para generador: " +
                            generador.getGeneratorId(), e);
                }
            });

            log.info("Finalizada generación de certificados del día");

        } catch (Exception e) {
            log.error("Error en generación masiva de certificados para fecha: " + fecha, e);
            throw new RuntimeException("Error generando certificados del día", e);
        }
    }

    @Override
    public void generarCertificadoManual(Long generadorId, LocalDate fecha) {
        try {
            log.info("Generando certificado manual para generador {} y fecha {}", generadorId, fecha);

            List<SealEntity> precintos = sealRepository.findByStateAndFechaTratamientoAndGeneratorEntity_GeneratorId(
                    SealStatus.TRATADO, fecha, generadorId);

            if (precintos.isEmpty()) {
                log.warn("No hay precintos tratados para generador {} en fecha {}", generadorId, fecha);
                return;
            }

            GeneratorEntity generador = precintos.get(0).getGeneratorEntity();
            enviarCertificadoPorEmail(generador, precintos, fecha);

            log.info("Certificado manual generado y enviado para generador: {}", generador.getName());

        } catch (Exception e) {
            log.error("Error generando certificado manual para generador: " + generadorId, e);
            throw new RuntimeException("Error generando certificado manual", e);
        }
    }

    private void enviarCertificadoPorEmail(GeneratorEntity generador, List<SealEntity> precintos, LocalDate fecha) {
        try {
            if (generador.getEmail() == null || generador.getEmail().trim().isEmpty()) {
                log.warn("Generador {} no tiene email configurado", generador.getName());
                return;
            }

            // Generar número de certificado único
            String numeroCertificado = generarNumeroCertificado(fecha, generador);

            String asunto = String.format("Certificado de Tratamiento de Residuos - %s",
                    fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            String cuerpo = construirCuerpoEmailResumido(generador, precintos, fecha, numeroCertificado);

            // Generar PDF como bytes
            byte[] certificadoPdf = generarCertificadoPDF(generador, precintos, fecha, numeroCertificado);

            String nombreAdjunto = String.format("Certificado_%s_%s.pdf",
                    numeroCertificado,
                    fecha.format(DateTimeFormatter.ofPattern("yyyyMMdd")));

            // Enviar con adjunto PDF
            envioCorreosService.enviarCorreoConAdjunto(
                    generador.getEmail(),
                    asunto,
                    cuerpo,
                    nombreAdjunto,
                    certificadoPdf
            );

            log.info("Certificado PDF enviado por email a: {} - {}",
                    generador.getName(), generador.getEmail());

        } catch (Exception e) {
            log.error("Error enviando certificado PDF por email a generador: " +
                    generador.getGeneratorId(), e);
        }
    }

    private String construirCuerpoEmailResumido(GeneratorEntity generador, List<SealEntity> precintos,
                                                LocalDate fecha, String numeroCertificado) {
        StringBuilder cuerpo = new StringBuilder();

        Double pesoTotal = precintos.stream()
                .mapToDouble(seal -> seal.getPeso() != null ? seal.getPeso() : 0.0)
                .sum();

        cuerpo.append("Estimado/a ").append(generador.getName()).append(",\n\n");

        cuerpo.append("Le informamos que se ha completado exitosamente el tratamiento de sus residuos patológicos.\n\n");

        cuerpo.append("RESUMEN:\n");
        cuerpo.append("=========\n");
        cuerpo.append("• Certificado N°: ").append(numeroCertificado).append("\n");
        cuerpo.append("• Fecha de tratamiento: ").append(fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        cuerpo.append("• Cantidad de precintos tratados: ").append(precintos.size()).append("\n");
        cuerpo.append("• Peso total procesado: ").append(pesoTotal).append(" kg\n\n");

        cuerpo.append("En el archivo adjunto encontrará el certificado oficial con todos los detalles ");
        cuerpo.append("del tratamiento realizado.\n\n");

        cuerpo.append("Este certificado cumple con la normativa vigente (Ley 24.051) y certifica ");
        cuerpo.append("el correcto tratamiento y disposición final de los residuos patológicos.\n\n");

        cuerpo.append("Gracias por confiar en nuestros servicios.\n\n");
        cuerpo.append("Saludos cordiales,\n\n");
        cuerpo.append("AESA Misiones S.A.\n");
        cuerpo.append("Gestión de Residuos Patológicos\n");
        cuerpo.append("Sistema de Trazabilidad");

        return cuerpo.toString();
    }

    private byte[] generarCertificadoPDF(GeneratorEntity generador, List<SealEntity> precintos,
                                         LocalDate fecha, String numeroCertificado) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Configurar fuentes
            PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Header con logo/título
            Paragraph header = new Paragraph("AESA MISIONES S.A.")
                    .setFont(fontBold)
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5);
            document.add(header);

            Paragraph subheader = new Paragraph("CERTIFICADO DE TRATAMIENTO DE RESIDUOS PATOLÓGICOS")
                    .setFont(fontBold)
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(subheader);

            // Información del certificado
            Table infoTable = new Table(2);
            infoTable.setWidth(UnitValue.createPercentValue(100));

            infoTable.addCell(new Cell().add(new Paragraph("N° Certificado:").setFont(fontBold)));
            infoTable.addCell(new Cell().add(new Paragraph(numeroCertificado).setFont(fontRegular)));

            infoTable.addCell(new Cell().add(new Paragraph("Fecha de Tratamiento:").setFont(fontBold)));
            infoTable.addCell(new Cell().add(new Paragraph(fecha.format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"))).setFont(fontRegular)));

            infoTable.addCell(new Cell().add(new Paragraph("Fecha de Emisión:").setFont(fontBold)));
            infoTable.addCell(new Cell().add(new Paragraph(LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).setFont(fontRegular)));

            document.add(infoTable);
            document.add(new Paragraph("\n"));

            // Información del generador
            Paragraph generadorTitle = new Paragraph("DATOS DEL GENERADOR")
                    .setFont(fontBold)
                    .setFontSize(12)
                    .setMarginBottom(10);
            document.add(generadorTitle);

            Table generadorTable = new Table(2);
            generadorTable.setWidth(UnitValue.createPercentValue(100));

            generadorTable.addCell(new Cell().add(new Paragraph("Nombre:").setFont(fontBold)));
            generadorTable.addCell(new Cell().add(new Paragraph(
                    generador.getName() != null ? generador.getName() : "N/A").setFont(fontRegular)));

            generadorTable.addCell(new Cell().add(new Paragraph("Domicilio:").setFont(fontBold)));
            generadorTable.addCell(new Cell().add(new Paragraph(
                    generador.getAddress() != null ? generador.getAddress() : "N/A").setFont(fontRegular)));

            generadorTable.addCell(new Cell().add(new Paragraph("Tipo:").setFont(fontBold)));
            generadorTable.addCell(new Cell().add(new Paragraph(
                    generador.getType() != null ? generador.getType() : "N/A").setFont(fontRegular)));

            generadorTable.addCell(new Cell().add(new Paragraph("Email:").setFont(fontBold)));
            generadorTable.addCell(new Cell().add(new Paragraph(
                    generador.getEmail() != null ? generador.getEmail() : "N/A").setFont(fontRegular)));

            document.add(generadorTable);
            document.add(new Paragraph("\n"));

            // Resumen del tratamiento
            Paragraph resumenTitle = new Paragraph("RESUMEN DEL TRATAMIENTO")
                    .setFont(fontBold)
                    .setFontSize(12)
                    .setMarginBottom(10);
            document.add(resumenTitle);

            Table resumenTable = new Table(2);
            resumenTable.setWidth(UnitValue.createPercentValue(100));

            Double pesoTotal = precintos.stream()
                    .mapToDouble(seal -> seal.getPeso() != null ? seal.getPeso() : 0.0)
                    .sum();

            resumenTable.addCell(new Cell().add(new Paragraph("Cantidad de Precintos:").setFont(fontBold)));
            resumenTable.addCell(new Cell().add(new Paragraph(String.valueOf(precintos.size())).setFont(fontRegular)));

            resumenTable.addCell(new Cell().add(new Paragraph("Peso Total Tratado:").setFont(fontBold)));
            resumenTable.addCell(new Cell().add(new Paragraph(pesoTotal + " kg").setFont(fontRegular)));

            document.add(resumenTable);
            document.add(new Paragraph("\n"));

            // Detalle de precintos
            if (!precintos.isEmpty()) {
                Paragraph precintosTitle = new Paragraph("DETALLE DE PRECINTOS TRATADOS")
                        .setFont(fontBold)
                        .setFontSize(12)
                        .setMarginBottom(10);
                document.add(precintosTitle);

                Table precintosTable = new Table(3);
                precintosTable.setWidth(UnitValue.createPercentValue(100));

                // Headers
                precintosTable.addHeaderCell(new Cell().add(new Paragraph("Código QR").setFont(fontBold)));
                precintosTable.addHeaderCell(new Cell().add(new Paragraph("N° Precinto").setFont(fontBold)));
                precintosTable.addHeaderCell(new Cell().add(new Paragraph("Peso (kg)").setFont(fontBold)));

                // Data
                precintos.forEach(precinto -> {
                    precintosTable.addCell(new Cell().add(new Paragraph(
                            precinto.getQrContent() != null ? precinto.getQrContent() : "N/A").setFont(fontRegular)));
                    precintosTable.addCell(new Cell().add(new Paragraph(
                            precinto.getSealNumber() != null ? precinto.getSealNumber() : "N/A").setFont(fontRegular)));
                    precintosTable.addCell(new Cell().add(new Paragraph(
                            precinto.getPeso() != null ? precinto.getPeso().toString() : "0.0").setFont(fontRegular)));
                });

                document.add(precintosTable);
                document.add(new Paragraph("\n"));
            }

            // Footer
            Paragraph footer = new Paragraph(
                    "Este certificado cumple con la normativa vigente (Ley 24.051) y certifica el correcto " +
                            "tratamiento y disposición final de los residuos patológicos mencionados.")
                    .setFont(fontRegular)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.JUSTIFIED)
                    .setMarginTop(20);
            document.add(footer);

            Paragraph firma = new Paragraph("AESA Misiones S.A. - Sistema de Trazabilidad de Residuos")
                    .setFont(fontBold)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(30);
            document.add(firma);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generando PDF del certificado para generador: " + generador.getGeneratorId(), e);
            // Fallback: generar un PDF simple
            return generarPDFSimple(generador, precintos, fecha, numeroCertificado);
        }
    }

    private byte[] generarPDFSimple(GeneratorEntity generador, List<SealEntity> precintos,
                                    LocalDate fecha, String numeroCertificado) {
        try {
            Double pesoTotal = precintos.stream()
                    .mapToDouble(seal -> seal.getPeso() != null ? seal.getPeso() : 0.0)
                    .sum();

            String contenido = String.format(
                    "CERTIFICADO DE TRATAMIENTO\n\n" +
                            "N° Certificado: %s\n" +
                            "Fecha: %s\n" +
                            "Generador: %s\n" +
                            "Precintos: %d\n" +
                            "Peso Total: %.2f kg\n\n" +
                            "AESA Misiones S.A.",
                    numeroCertificado,
                    fecha,
                    generador.getName(),
                    precintos.size(),
                    pesoTotal
            );
            return contenido.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error generando PDF simple", e);
            return "Error generando certificado".getBytes(StandardCharsets.UTF_8);
        }
    }

    private String generarNumeroCertificado(LocalDate fecha, GeneratorEntity generador) {
        return String.format("CERT-%s-%d-%d",
                fecha.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                generador.getGeneratorId(),
                System.currentTimeMillis() % 10000);
    }
}
package ar.edu.utn.frc.tup.lciii.services.impl;

import ar.edu.utn.frc.tup.lciii.models.Generator;
import ar.edu.utn.frc.tup.lciii.services.ContractService;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class ContractServiceImpl implements ContractService {

    @Override
    public byte[] generarContratoComercial(Generator generator) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            // Título principal
            document.add(new Paragraph("ACUERDO COMERCIAL - PROVISIÓN DE BOLSAS A GENERADORES PRIVADOS")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(14)
                    .setMarginBottom(10));

            // Subtítulo
            document.add(new Paragraph("SOLICITUD DE PROVISIÓN DE BOLSAS DE RESIDUOS PATOLÓGICOS\n" +
                    "(CORRESPONDIENTE AL SISTEMA DE TRANSPORTE, TRATAMIENTO Y DISPOSICIÓN\n" +
                    "FINAL DE RESIDUOS PATOLÓGICOS – CONCESIONADO POR AESA MISIONES S.A.)")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(11)
                    .setMarginBottom(15));

            // Encabezado del contrato
            String fechaActual = LocalDate.now().format(DateTimeFormatter.ofPattern("dd"));
            String mesActual = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM"));
            String anioActual = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));

            document.add(new Paragraph("En la Ciudad de Posadas, Capital de la Provincia de Misiones, a los " +
                    fechaActual + " días de " + mesActual + " de " + anioActual + ", entre " +
                    generator.getName() + " (en adelante el \"solicitante\" o el \"generador\"), por una parte y por la otra, " +
                    "la Empresa AESA MISIONES S.A. (en adelante la \"Empresa\"), representada en este acto por " +
                    "……………………………………………………, en su carácter de Concesionaria del Contrato suscripto con la " +
                    "Provincia de Misiones, en el marco de la Licitación Pública N° 04/1999 y sus modificaciones, " +
                    "para el Transporte, Tratamiento y Disposición Final de Residuos Sólidos Urbanos y Patológicos, " +
                    "representada en este acto por su apoderado Ing. GABRIEL OMAR KELLER, conforme Poder General " +
                    "otorgado por Escritura Nº 190 de fecha 13 de diciembre de 2022, conjuntamente \"Las Partes\", " +
                    "acuerdan suscribir el presente acuerdo comercial de provisión de bolsas y precintos para la " +
                    "gestión de residuos patológicos.")
                    .setTextAlignment(TextAlignment.JUSTIFIED)
                    .setMarginBottom(15));

            // PRELIMINAR
            document.add(new Paragraph("PRELIMINAR.")
                    .setBold()
                    .setUnderline()
                    .setMarginBottom(10));

            document.add(new Paragraph("En el marco de la Licitación Pública N° 04/99 para la concesión de los servicios " +
                    "de transporte, tratamiento y disposición final de los residuos sólidos urbanos, asimilables y " +
                    "patológicos, de las ciudades de Posadas, Garupá, Candelaria, Santa Ana, San Ignacio, " +
                    "Gobernador Roca, Santo Pipo y Jardín América, AESA MISIONES S.A. resultó adjudicataria, " +
                    "celebrándose el correspondiente contrato el 2 de marzo de 2000, entre la provincia de Misiones " +
                    "por una parte y por la otra parte Fomento de Construcciones y Contratas S.A. y AESA, " +
                    "Aseo y Ecología S.A. (luego AESA MISIONES S.A. conforme el compromiso de constituir una " +
                    "sociedad anónima para asumir la Concesión).")
                    .setTextAlignment(TextAlignment.JUSTIFIED)
                    .setMarginBottom(10));

            document.add(new Paragraph("El 23 de diciembre de 2002, las partes convinieron celebrar la extensión del " +
                    "contrato a fin de ampliar a toda la Provincia de Misiones \"… la prestación del Servicio de " +
                    "Transporte, Tratamiento y Disposición Final de los Residuos Sólidos Urbanos y Asimilables y " +
                    "de los Residuos Patológicos generados en los Centros de Atención Pública de la Salud, ...\"")
                    .setTextAlignment(TextAlignment.JUSTIFIED)
                    .setMarginBottom(10));

            document.add(new Paragraph("En razón de ello, es que, a la firma del presente, todos los generadores " +
                    "privados/municipalidades abonan de forma diferencial para su transporte, tratamiento y " +
                    "disposición final una tarifa por cobro del servicio a particulares. Ello, de conformidad " +
                    "con lo dispuesto en el Art. 2.14 del Contrato Original.")
                    .setTextAlignment(TextAlignment.JUSTIFIED)
                    .setMarginBottom(10));

            document.add(new Paragraph("El cobro de la tarifa a todos los generadores privados de residuos patogénicos, " +
                    "encuentra fundamento en el cumplimiento del principio de trato igualitario y garantiza la " +
                    "continuidad, regularidad y calidad del servicio.")
                    .setTextAlignment(TextAlignment.JUSTIFIED)
                    .setMarginBottom(10));

            document.add(new Paragraph("Conforme lo expuesto, resulta necesario que cada Municipio preste el servicio de " +
                    "recolección y traslado de residuos patológicos a generadores privados de su territorio " +
                    "-llevando esta última su propio control-, para que luego sean transferidos a AESA Misiones S.A. " +
                    "a fin de que la empresa brinde el servicio de recepción de residuos sólidos patológicos en la " +
                    "estación de transferencia, para su posterior transporte, tratamiento y disposición final.")
                    .setTextAlignment(TextAlignment.JUSTIFIED)
                    .setMarginBottom(15));

            document.add(new Paragraph("Ante lo expuesto, las Partes acuerdan suscribir el presente contrato de " +
                    "conformidad a las siguientes cláusulas:")
                    .setTextAlignment(TextAlignment.JUSTIFIED)
                    .setMarginBottom(15));

            // Cláusulas numeradas
            agregarClausula(document, 1, "AESA Misiones S.A. proveerá de bolsas y precintos para la gestión de residuos " +
                    "patológicos a los generadores adherentes al presente contrato que hayan sido habilitados " +
                    "previamente por su municipio.");

            agregarClausula(document, 2, "El municipio gestionará la recolección de los residuos patológicos privados " +
                    "en forma domiciliaria dentro de su límite territorial y el traslado de los mismos hasta el " +
                    "lugar de su recepción por parte de AESA Misiones S.A.");

            agregarClausula(document, 3, "Se consideran generadores de residuos patológicos los establecimientos " +
                    "asistenciales médicos, odontológicos, veterinarios, laboratorios de análisis clínicos, " +
                    "medicinales, farmacias, centros de investigación, gabinetes de enfermería y toda persona " +
                    "física o jurídica que los produzca a consecuencia de su actividad.");

            agregarClausula(document, 4, "Se consideran residuos patológicos a los desechos o elementos materiales " +
                    "en estado sólido, semisólido, líquido o gaseoso, que presenten características de toxicidad " +
                    "y/o actividad biológica, que puedan afectar directa o indirectamente a los seres vivos y/o " +
                    "causar contaminación del suelo, agua, atmósfera o al medio ambiente en general.");

            agregarClausula(document, 5, "Los residuos a tratar por AESA Misiones S.A. serán los patológicos de " +
                    "corriente Y01, con características de peligrosidad H6.2 con exclusión de líquidos y piezas " +
                    "anatómicas. Cualquier transgresión cometida por el generador lo hará civil y penalmente " +
                    "responsable por los daños causados al personal, instalaciones y/o equipamiento de AESA Misiones S.A.");

            agregarClausula(document, 6, "El generador adherido al presente deberá respetar y cumplir con las normas " +
                    "básicas para el correcto tratamiento de los residuos patogénicos generados según lo indicado " +
                    "en el Anexo I.");

            agregarClausula(document, 7, "El generador deberá adquirir y garantizar que sean utilizadas las bolsas y " +
                    "precintos provistos por AESA Misiones S.A. abonando por cada bolsa el precio fijado en la " +
                    "cláusula siguiente, que será actualizado e informado por AESA Misiones S.A. en función de lo " +
                    "determinado por el ente regulador de la concesión: Instituto Provincial de Desarrollo " +
                    "Habitacional de la Provincia de Misiones (IPRODHA). Las mismas serán solicitadas según " +
                    "necesidad del generador únicamente por vía correo electrónico a recepcion.misiones@veolia.com " +
                    "o telefónicamente al +54 9 376 - 4108604 y se entregarán en las oficinas de 25 de Mayo 1840, " +
                    "of. 2, ciudad de Posadas o con envío a cargo del solicitante a través de la empresa de " +
                    "transporte designada por AESA Misiones S.A.");

            agregarClausula(document, 8, "Las bolsas referidas en el punto anterior serán de una medida de 60 x 82 " +
                    "centímetros para las bolsas grandes con una capacidad máxima de 5 Kg. y de una medida de " +
                    "50 x 70 centímetros para las bolsas chicas con una capacidad máxima de 2,5 Kg., ambas de " +
                    "color rojo y de 120 micrones de espesor, con indicaciones impresas de riesgo biológico y " +
                    "logotipo de AESA Misiones S.A. Junto con las bolsas se proveerá igual cantidad de precintos " +
                    "plásticos para el correcto cierre de las mismas, de acuerdo a lo establecido en el Anexo I " +
                    "de la presente solicitud. El precio vigente al momento de la firma del presente acta acuerdo " +
                    "es de $4.156,00 + IVA por la bolsa grande y $2.301,00 + IVA por la bolsa chica.");

            agregarClausula(document, 9, "La facturación de AESA Misiones S.A. será generada en el momento en que se " +
                    "genere una operación de compra de bolsas por parte del solicitante. Las fechas de pago de las " +
                    "facturas emitidas por la Empresa AESA Misiones S.A. no deberán superar los 30 días corridos " +
                    "desde la fecha de emisión del comprobante mencionado, y deberán ser abonados a la CC en pesos " +
                    "Nro. 300100080033046 del Banco Macro S.A. con número de CBU 2850001030000800330461 a nombre " +
                    "de AESA MISIONES SA CUIT 30-70714831-0. La falta de pago en término de las facturas hará " +
                    "incurrir al generador / solicitante en mora automática y de pleno derecho, dando derecho a " +
                    "AESA Misiones S.A. a cobrar intereses moratorios iguales a 1,5 veces la tasa de descuento " +
                    "de documentos del Banco de la Nación Argentina, sin perjuicio de la facultad de resolver el " +
                    "Acuerdo, como así también la facultad de suspender automáticamente la prestación del Servicio.");

            agregarClausula(document, 10, "La duración del presente acuerdo comercial tendrá vigencia hasta el 31 de " +
                    "diciembre de 2024, pudiendo cualquiera de las partes en cualquier momento rescindir, previo " +
                    "aviso fehaciente, con treinta días de anticipación, sin que la rescisión sin causa otorgue a " +
                    "ninguna de las partes derecho a iniciar reclamo alguno contra la otra Parte. Vencido dicho " +
                    "plazo, deberá adherirse al servicio integral de residuos patológicos, adquisición de bolsas " +
                    "y recolección, en la Municipalidad de la localidad donde realice su actividad.");

            agregarClausula(document, 11, "AESA Misiones S.A. se reserva el derecho de rescindir el acuerdo en " +
                    "cualquier momento y en forma automática, cuando tenga conocimiento de circunstancias o hechos " +
                    "que, a su juicio, afecten el prestigio, crédito o concepto público de la empresa y/o cuando el " +
                    "generador incumpla cualquiera de las obligaciones establecidas en el presente.");

            agregarClausula(document, 12, "La responsabilidad total de AESA Misiones S.A., en cualquier eventualidad, " +
                    "por las pérdidas, daños y perjuicios que surjan, se relacionen o resulten del presente acuerdo " +
                    "y de los Servicios ejecutados por AESA Misiones S.A., tendrá como límite máximo el 10 % del " +
                    "precio efectivamente facturado y percibido por AESA Misiones S.A. por la prestación del presente " +
                    "acuerdo. Sin perjuicio de lo dicho precedentemente, AESA Misiones S.A. no será responsable por " +
                    "lucro cesante o cualquier pérdida o daño indirecto (consecuencias mediatas o causales), " +
                    "incluyendo pero no limitado a la falta de producción, la pérdida de explotación, pérdidas de " +
                    "pedidos o negocios, multas o reclamos de terceros, costo de capital, que pudiera sufrir el " +
                    "municipio y/o los generadores por el cumplimiento o incumplimiento de los Servicios objetos " +
                    "del presente acuerdo, así como por cualquier pérdida o daño indirecto; limitando cualquier " +
                    "indemnización al daño emergente, dentro del tope previsto en el presente punto.");

            agregarClausula(document, 13, "Los términos y condiciones que rigen el acuerdo podrán ser modificados por " +
                    "AESA Misiones S.A. en cualquier momento, mediante preaviso de 30 días a la fecha de entrada " +
                    "en vigor de la modificación. Si dentro de tal plazo el generador adherente no opta por la " +
                    "rescisión, se entenderá que las modificaciones introducidas son aceptadas y de cumplimiento " +
                    "obligatorio. Asimismo todo cambio a la información declarada por el generador en el Anexo II " +
                    "deberá ser notificado con el mismo plazo.");

            agregarClausula(document, 14, "El generador adherente constituye domicilio especial declarado en el Anexo II " +
                    "del presente acuerdo. Cualquier divergencia vinculada a la interpretación de esta solicitud, " +
                    "será resuelta por los Tribunales Ordinarios de Posadas, con exclusión de cualquier otro fuero " +
                    "o jurisdicción.");

            agregarClausula(document, 15, "El generador se considerará adherido al sistema a partir de que AESA " +
                    "Misiones S.A. le comunique la aceptación del presente.");

            agregarClausula(document, 16, "El presente acuerdo comercial es de aplicación únicamente dentro del " +
                    "municipio de ………-");

            // Espacio para firmas
            document.add(new Paragraph("\n\n"));

            // Tabla para firmas
            Table firmasTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
            firmasTable.setWidth(UnitValue.createPercentValue(100));

            Cell firmaGenerador = new Cell()
                    .add(new Paragraph("Firma y Aclaración\n\nGenerador solicitante")
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBorder(null)
                    .setPadding(20);

            Cell firmaAesa = new Cell()
                    .add(new Paragraph("Firma y Aclaración\n\nApoderado AESA Misiones S.A.")
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBorder(null)
                    .setPadding(20);

            firmasTable.addCell(firmaGenerador);
            firmasTable.addCell(firmaAesa);

            document.add(firmasTable);

            // Nueva página para Anexo I
            document.add(new com.itextpdf.layout.element.AreaBreak());

            // ANEXO I
            document.add(new Paragraph("ANEXO I – NORMAS BÁSICAS PARA LA GESTIÓN DE RESIDUOS PATOLÓGICOS A ENTREGAR A AESA MISIONES S.A.")
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(12)
                    .setMarginBottom(15));

            agregarNormaBasica(document, 1, "Las bolsas para disposición de los residuos patológicos deberán ser las " +
                    "provistas por AESA Misiones S.A.");

            agregarNormaBasica(document, 2, "Las bolsas con residuos, para su retiro y disposición, deben contener " +
                    "únicamente residuos patológicos sólidos, Corriente Y01, peligrosidad H6.2 con exclusión de " +
                    "piezas anatómicas. Está prohibido incluir dentro de las bolsas: medicamentos, insumos, frascos, " +
                    "combustibles, aerosoles, envases de vidrio cerrados, residuos domiciliarios u otros elementos/" +
                    "residuos que no sean patológicos (sólidos). No se admiten restos de material orgánico ni " +
                    "partes animales o humanas.");

            agregarNormaBasica(document, 3, "Cada bolsa con residuos patológicos, para su retiro y disposición, deberá " +
                    "estar correctamente precintada de manera individual, preservada en buenas condiciones y sin " +
                    "roturas. Las bolsas deben ser utilizadas como medio de contención final del proceso de recolección.");

            agregarNormaBasica(document, 4, "La bolsa con residuos patológicos debe contar con un peso máximo de 5 kg " +
                    "para bolsas de 60x82cm y 2,50Kg para bolsas de 50x70cm, y no superando en volumen el 3/4 de " +
                    "la capacidad/tamaño de la bolsa.");

            agregarNormaBasica(document, 5, "Dentro de las bolsas los elementos del tipo corto/punzante (agujas, " +
                    "bisturís, etc.) deben estar contenidos en recipientes del tipo descartable, resistentes que " +
                    "soportan golpes y/o perforaciones. NO permitiendo que estos elementos se expongan hacia el " +
                    "exterior, comprometiendo la seguridad de las personas.");

            agregarNormaBasica(document, 6, "Las bolsas con residuos patológicos que no cumplan con las condiciones/" +
                    "parámetros expuestos previamente, no serán recibidas por AESA Misiones S.A. Sin perjuicio de " +
                    "lo cual la recepción de las bolsas no implicará la conformidad de AESA Misiones S.A. ni eximirá " +
                    "de responsabilidad alguna al generador y municipio.");

            agregarNormaBasica(document, 7, "Será responsabilidad del generador y municipio que las bolsas y residuos " +
                    "se encuentren acondicionados conforme las condiciones descritas en el presente. AESA Misiones S.A. " +
                    "podrá realizar una inspección y control al momento de recepción en el punto acordado o en el " +
                    "momento previo a su tratamiento en las instalaciones correspondientes. Estas acciones efectuadas " +
                    "por AESA Misiones S.A. no eximirá ni limitará la responsabilidad del generador y municipio por " +
                    "la incorrecta disposición de los residuos y por los daños y perjuicios que ello pudiera causar " +
                    "a AESA Misiones S.A., a su personal, sus equipos e instalaciones, y/o a terceros y sus bienes.");

            agregarNormaBasica(document, 8, "Al momento que la Municipalidad entregue las bolsas precintadas (en el " +
                    "lugar a convenir) y estas sean recepcionadas por AESA Misiones, la Municipalidad deberá firmar " +
                    "una planilla/registro de entrega de las mismas, donde se especificará/detallará información " +
                    "como ser el tipo de bolsas, cantidad, estado, fecha, hora, datos del chofer y móvil. AESA " +
                    "Misiones S.A. llevará un control de la cantidad de bolsas recepcionadas para su tratamiento " +
                    "y disposición final.");

            agregarNormaBasica(document, 9, "Ante la necesidad de brindar sugerencias y/o entablar reclamos por " +
                    "irregularidades en el suministro de bolsas y/o servicio, las vías de contacto válidas con " +
                    "AESA Misiones SA, son:\n" +
                    "i. Por Whatsapp: +54 9 376 – 5172011\n" +
                    "ii. Por correo electrónico a la casilla reclamos.aesamisiones@veolia.com");

            // Nueva página para Anexo II
            document.add(new com.itextpdf.layout.element.AreaBreak());

            // ANEXO II - Información del generador
            document.add(new Paragraph("ANEXO II INFORMACIÓN DEL GENERADOR")
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(12)
                    .setMarginBottom(15));

            document.add(new Paragraph("DATOS BÁSICOS:")
                    .setBold()
                    .setMarginBottom(10));

            document.add(new Paragraph("RAZÓN SOCIAL: " + generator.getName())
                    .setMarginBottom(5));
            document.add(new Paragraph("RESPONSABLE: ………………………………………………………………………")
                    .setMarginBottom(5));
            document.add(new Paragraph("CONTACTO: " + generator.getContact())
                    .setMarginBottom(5));
            document.add(new Paragraph("DOMICILIO: " + generator.getAddress())
                    .setMarginBottom(5));
            document.add(new Paragraph("CALLE: ………………… NRO: ………………………………………")
                    .setMarginBottom(5));
            document.add(new Paragraph("PISO: ………………… DEPTO: ………………………………")
                    .setMarginBottom(5));
            document.add(new Paragraph("LOCALIDAD: ………………… C.P. ………………………")
                    .setMarginBottom(5));
            document.add(new Paragraph("PCIA: MISIONES")
                    .setMarginBottom(5));
            document.add(new Paragraph("TELÉFONO: ………………………………………………………")
                    .setMarginBottom(5));
            document.add(new Paragraph("CORREO ELECTRÓNICO: " + generator.getEmail())
                    .setMarginBottom(15));

            document.add(new Paragraph("DATOS IMPOSITIVOS:")
                    .setBold()
                    .setMarginBottom(10));

            document.add(new Paragraph("CUIT: ………………………………………………………………")
                    .setMarginBottom(5));
            document.add(new Paragraph("CATEGORÍA ANTE IVA: ………………………………………")
                    .setMarginBottom(15));

            document.add(new Paragraph("TIPO DE GENERADOR: " + generator.getType())
                    .setMarginBottom(15));

            // Documentación requerida
            document.add(new Paragraph("DOCUMENTACIÓN A ANEXAR:")
                    .setBold()
                    .setMarginBottom(10));

            document.add(new Paragraph("1. PERSONAS FÍSICAS:")
                    .setBold()
                    .setMarginBottom(5));

            document.add(new Paragraph("a) Fotocopia de DNI\n" +
                    "b) Constancia de CUIT vigente\n" +
                    "c) Certificados de no percepción y/o exención en IIBB\n" +
                    "d) Constancia de inscripción en IIBB: Contribuyente local, CM05 o Exento, según corresponda\n" +
                    "e) Habilitación municipal correspondiente a la actividad")
                    .setMarginBottom(10));

            document.add(new Paragraph("2. SOCIEDADES:")
                    .setBold()
                    .setMarginBottom(5));

            document.add(new Paragraph("a) Copia constancia de CUIT vigente\n" +
                    "b) Constancia de inscripción en AFIP (CUIT)\n" +
                    "c) Certificados de no percepción y/o exención en IIBB\n" +
                    "d) En caso de Sociedades poder que habilite la contratación\n" +
                    "e) Constancia de inscripción en IIBB: Contribuyente local, CM05 o Exento, según corresponda\n" +
                    "f) Habilitación municipal correspondiente a la actividad"));

            document.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando contrato PDF", e);
        }
    }

    private void agregarClausula(Document document, int numero, String texto) {
        document.add(new Paragraph(numero + ". " + texto)
                .setTextAlignment(TextAlignment.JUSTIFIED)
                .setMarginBottom(10)
                .setMarginLeft(20));
    }

    private void agregarNormaBasica(Document document, int numero, String texto) {
        document.add(new Paragraph(numero + ". " + texto)
                .setTextAlignment(TextAlignment.JUSTIFIED)
                .setMarginBottom(8)
                .setMarginLeft(15));
    }
}
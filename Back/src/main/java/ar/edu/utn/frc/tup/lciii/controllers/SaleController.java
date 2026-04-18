package ar.edu.utn.frc.tup.lciii.controllers;

import ar.edu.utn.frc.tup.lciii.dtos.SaleDto;
import ar.edu.utn.frc.tup.lciii.entities.GeneratorEntity;
import ar.edu.utn.frc.tup.lciii.entities.UserEntity;
import ar.edu.utn.frc.tup.lciii.models.Sale;
import ar.edu.utn.frc.tup.lciii.models.SealStatus;
import ar.edu.utn.frc.tup.lciii.repositories.GeneratorRepository;
import ar.edu.utn.frc.tup.lciii.repositories.SealRepository;
import ar.edu.utn.frc.tup.lciii.repositories.UserRepository;
import ar.edu.utn.frc.tup.lciii.services.JwtService;
import ar.edu.utn.frc.tup.lciii.services.MercadoPagoService;
import ar.edu.utn.frc.tup.lciii.services.SaleService;
import ar.edu.utn.frc.tup.lciii.services.impl.EnvioCorreosServiceImpl;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")

@RestController
@RequestMapping("Sale")
public class SaleController {
    @Autowired
    private SaleService saleService;
    @Autowired
    private EnvioCorreosServiceImpl emailService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private MercadoPagoService mercadoPagoService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SealRepository sealRepository;

    @Autowired
    private GeneratorRepository generatorRepository;

    public ResponseEntity<SaleDto> registerSale(@RequestBody SaleDto saleDto) {
        Sale sale = saleService.registerSale(saleDto);
        return ResponseEntity.ok(modelMapper.map(sale, SaleDto.class));
    }
    @GetMapping("/webhook-test")
    public ResponseEntity<String> webhookTest() {
        System.out.println("=== WEBHOOK GET TEST ACCESSED ===");
        return ResponseEntity.ok("Webhook endpoint is reachable via GET");
    }

    @PostMapping("/webhook-test")
    public ResponseEntity<String> webhookTestPost(@RequestBody(required = false) String body) {
        System.out.println("=== WEBHOOK POST TEST ACCESSED ===");
        System.out.println("Body: " + body);
        return ResponseEntity.ok("Webhook endpoint is reachable via POST");
    }
    @PostMapping("/webhook-manual")
    public ResponseEntity<String> webhookManual(@RequestParam String paymentId) {
        try {
            System.out.println("=== WEBHOOK MANUAL TRIGGERED ===");
            System.out.println("Payment ID: " + paymentId);

            PaymentClient paymentClient = new PaymentClient();
            Payment payment = paymentClient.get(Long.parseLong(paymentId));

            System.out.println("Estado: " + payment.getStatus());
            System.out.println("External Reference: " + payment.getExternalReference());

            if ("approved".equals(payment.getStatus())) {
                String externalReference = payment.getExternalReference();

                if (externalReference != null && externalReference.contains("-")) {
                    String[] parts = externalReference.split("-");
                    Long saleId = Long.parseLong(parts[0]);
                    Long generatorId = Long.parseLong(parts[1]);

                    System.out.println("Confirmando Sale ID: " + saleId + ", Generator ID: " + generatorId);

                    saleService.confirmSale(saleId, generatorId);
                    System.out.println("✅ Venta confirmada manualmente");

                    return ResponseEntity.ok("Venta confirmada exitosamente - Sale ID: " + saleId);
                } else {
                    return ResponseEntity.badRequest().body("External reference inválido: " + externalReference);
                }
            } else {
                return ResponseEntity.badRequest().body("Pago no aprobado. Estado: " + payment.getStatus());
            }

        } catch (Exception e) {
            System.err.println("Error en webhook manual: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/confirm-last-sale")
    public ResponseEntity<String> confirmLastSale(@RequestParam Long generatorId) {
        try {
            System.out.println("=== CONFIRMING LAST SALE ===");
            System.out.println("Generator ID: " + generatorId);

            Long lastSaleId = saleService.getLastPendingSaleId(generatorId);

            if (lastSaleId != null) {
                saleService.confirmSale(lastSaleId, generatorId);
                System.out.println("✅ Última venta confirmada y cambiada a PAID - Sale ID: " + lastSaleId);
                return ResponseEntity.ok("Venta confirmada exitosamente - Sale ID: " + lastSaleId + " estado: PAID");
            } else {
                return ResponseEntity.badRequest().body("No hay ventas con estado PENDING para el generador: " + generatorId);
            }

        } catch (Exception e) {
            System.err.println("Error confirmando última venta: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    @PostMapping("/enviar")
    public String enviarCorreo(
            @RequestParam String destinatario,
            @RequestParam String asunto,
            @RequestParam String mensaje) {
        emailService.enviarCorreo(destinatario, asunto, mensaje);
        return "Correo enviado con éxito";
    }
    @PostMapping("/crear-preferencia")
    public Map<String, Object> createPreference(@RequestParam String title,
                                                @RequestParam BigDecimal unitPrice,
                                                @RequestParam Integer quantity,
                                                @RequestHeader("Authorization") String authHeader) throws MPException, MPApiException {

        String token = authHeader.replace("Bearer ", "");
        String username = jwtService.extractUsername(token);

        GeneratorEntity generator = generatorRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        Long generatorId = generator.getGeneratorId();

        // ✅ VERIFICAR PRECINTOS DISPONIBLES ANTES DE PERMITIR EL PAGO
        long availableSeals = sealRepository.countByState(SealStatus.DISPONIBLE);

        if (availableSeals < quantity) {
            throw new RuntimeException("No hay suficientes precintos disponibles. " +
                    "Disponibles: " + availableSeals + ", Solicitados: " + quantity);
        }

        // Solo si hay precintos suficientes, crear venta y preferencia
        SaleDto saleDto = new SaleDto();
        saleDto.setQuantity(quantity);
        saleDto.setGeneratorId(generatorId);

        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(quantity));
        saleDto.setTotalBuy(total.doubleValue());

        Sale sale = saleService.registerSale(saleDto);
        Long saleId = sale.getId();

        Preference pref = mercadoPagoService.createPreference(title, unitPrice, quantity, saleId, generatorId);

        System.out.println("=== PREFERENCIA CREADA ===");
        System.out.println("Sale ID: " + saleId);
        System.out.println("Generator ID: " + generatorId);
        System.out.println("Precintos disponibles: " + availableSeals);

        Map<String, Object> response = new HashMap<>();
        response.put("id", pref.getId());
        response.put("initPoint", pref.getSandboxInitPoint());
        response.put("saleId", saleId);
        response.put("generatorId", generatorId);

        return response;
    }
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody Map<String, Object> notification) {
        try {
            System.out.println("=== WEBHOOK MERCADOPAGO RECIBIDO ===");
            System.out.println("Notificación completa: " + notification);

            String type = (String) notification.get("type");

            if ("payment".equals(type)) {
                Map<String, Object> data = (Map<String, Object>) notification.get("data");
                String paymentId = (String) data.get("id");

                System.out.println("Payment ID recibido: " + paymentId);

                if ("123456".equals(paymentId)) {
                    System.out.println("⚠️ Webhook de simulación ignorado");
                    return ResponseEntity.ok("SIMULATION_IGNORED");
                }

                try {
                    PaymentClient paymentClient = new PaymentClient();
                    Payment payment = paymentClient.get(Long.parseLong(paymentId));

                    System.out.println("=== DEBUG COMPLETO PAYMENT ===");
                    System.out.println("Payment ID: " + payment.getId());
                    System.out.println("Estado: " + payment.getStatus());
                    System.out.println("Status Detail: " + payment.getStatusDetail());
                    System.out.println("External Reference: " + payment.getExternalReference());
                    System.out.println("Description: " + payment.getDescription());
                    System.out.println("Payment Method ID: " + payment.getPaymentMethodId());
                    System.out.println("Payment Type ID: " + payment.getPaymentTypeId());
                    System.out.println("Issuer ID: " + payment.getIssuerId());
                    System.out.println("Installments: " + payment.getInstallments());
                    System.out.println("Transaction Amount: " + payment.getTransactionAmount());
                    System.out.println("Currency ID: " + payment.getCurrencyId());
                    System.out.println("Date Created: " + payment.getDateCreated());
                    System.out.println("Date Approved: " + payment.getDateApproved());
                    System.out.println("Date Last Updated: " + payment.getDateLastUpdated());

                    // Verificar si tiene payer info
                    if (payment.getPayer() != null) {
                        System.out.println("Payer ID: " + payment.getPayer().getId());
                        System.out.println("Payer Email: " + payment.getPayer().getEmail());
                    }

                    // Verificar si tiene order info
                    if (payment.getOrder() != null) {
                        System.out.println("Order ID: " + payment.getOrder().getId());
                        System.out.println("Order Type: " + payment.getOrder().getType());
                    }

                    // Verificar metadata si existe
                    if (payment.getMetadata() != null) {
                        System.out.println("Metadata: " + payment.getMetadata());
                    }

                    // Verificar additional info si existe
                    if (payment.getAdditionalInfo() != null) {
                        System.out.println("Additional Info: " + payment.getAdditionalInfo());
                    }

                    System.out.println("========================");

                    if ("approved".equals(payment.getStatus())) {
                        String externalReference = payment.getExternalReference();

                        if (externalReference != null && !externalReference.trim().isEmpty() && externalReference.contains("-")) {
                            System.out.println("Procesando external reference: " + externalReference);

                            String[] parts = externalReference.split("-");
                            if (parts.length >= 2) {
                                try {
                                    Long saleId = Long.parseLong(parts[0].trim());
                                    Long generatorId = Long.parseLong(parts[1].trim());

                                    if (saleId > 0 && generatorId > 0) {
                                        saleService.confirmSale(saleId, generatorId);
                                        System.out.println("✅ Venta confirmada automáticamente - Sale ID: " + saleId);
                                    } else {
                                        System.err.println("❌ IDs inválidos");
                                    }
                                } catch (NumberFormatException e) {
                                    System.err.println("❌ Error parseando números: " + e.getMessage());
                                }
                            }
                        } else {
                            System.err.println("❌ External reference inválido: '" + externalReference + "'");
                            System.out.println("🔍 Intentando encontrar la venta por otros medios...");

                            // FALLBACK: Buscar la venta más reciente sin confirmar
                            // Esto es temporal hasta que resolvamos el external_reference
                            try {
                                // Buscar ventas pendientes recientes (últimos 10 minutos)
                                OffsetDateTime hace10min = OffsetDateTime.now().minusMinutes(10);
                                // List<Sale> ventasPendientes = saleRepository.findPendingSalesAfter(hace10min);
                                System.out.println("💡 Sugerencia: Implementar búsqueda de ventas pendientes como fallback");
                            } catch (Exception e) {
                                System.err.println("Error en fallback: " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception paymentError) {
                    System.err.println("❌ Error consultando pago: " + paymentError.getMessage());
                    paymentError.printStackTrace();
                }
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            System.err.println("❌ Error procesando webhook: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok("ERROR");
        }
    }
    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestBody Map<String,Long> body) {
        saleService.confirmSale(body.get("saleId"), body.get("generatorId"));
        return ResponseEntity.ok("Venta confirmada y precintos asignados");
    }
    @GetMapping("/historial-ventas")
    public ResponseEntity<List<SaleDto>> getSalesById(@RequestParam Long id) {
        List<Sale> sales = saleService.findByGenerator(id);
        List<SaleDto> saleDtos = new ArrayList<>();
        for (Sale sale : sales) {
            saleDtos.add(modelMapper.map(sale, SaleDto.class));
        }
        return ResponseEntity.ok(saleDtos);
    }
}

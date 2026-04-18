package ar.edu.utn.frc.tup.lciii.config;

import com.mercadopago.MercadoPagoConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
@Configuration
public class MercadoPagoConfiguration {
    @Value("${mercadopago.access.token}")
    private String accessToken;

    @Value("${mercadopago.public.key}")
    private String publicKey;

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(accessToken);
        System.out.println("=== MERCADOPAGO CONFIGURADO ===");
        System.out.println("Access Token: " + accessToken.substring(0, 20) + "...");
        System.out.println("Public Key: " + publicKey.substring(0, 20) + "...");
        System.out.println("===============================");
    }
}

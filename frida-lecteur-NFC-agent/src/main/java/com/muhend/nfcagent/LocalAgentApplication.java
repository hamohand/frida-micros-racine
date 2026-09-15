package com.muhend.nfcagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

@SpringBootApplication
public class LocalAgentApplication {

    public static void main(String[] args) {
        // Ajouter BouncyCastle pour le support cryptographique JMRTD
        Security.addProvider(new BouncyCastleProvider());
        
        // Forcer le port 8088 pour ne pas interférer avec le backend principal
        System.setProperty("server.port", "8088");
        SpringApplication.run(LocalAgentApplication.class, args);
    }

    // Autoriser le frontend Angular à contacter cet agent depuis localhost
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*") // En local, on peut être permissif
                        .allowedMethods("GET", "POST", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}

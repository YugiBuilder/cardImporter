package com.yugibuilder.cardimporter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Value("${ygoprodeck.api.url:https://db.ygoprodeck.com/api/v7/cardinfo.php}")
    private String apiUrl;

    /**
     * Configuration RestTemplate principale avec timeout et gestion d'erreurs optimisés
     * @Primary assure qu'il n'y aura pas de conflit avec d'autres beans RestTemplate
     */
    @Bean
    @Primary // CRUCIAL : Évite les conflits de beans
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri(getApiBaseUrl())
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(60))
                .build();
    }

    // CORRECTION: Logique corrigée pour getApiBaseUrl()
    private String getApiBaseUrl() {
        // Extrait l'URL de base (sans query parameters) pour rootUri
        if (apiUrl != null && !apiUrl.isEmpty()) {
            if (apiUrl.contains("?")) {
                return apiUrl.substring(0, apiUrl.indexOf("?"));
            }
            // CORRECTION CRITIQUE: Retourner l'URL complète si pas de paramètres
            return apiUrl;
        }
        return "https://db.ygoprodeck.com/api/v7";
    }
}
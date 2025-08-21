package com.yugibuilder.cardimporter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for Swagger/OpenAPI 3 with Spring Boot 3.4+
 * Updated to use the latest springdoc-openapi-starter-webmvc-ui
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI(
            @Value("${server.port:8080}") String serverPort,
            @Value("${spring.application.name:YugiBuilder Card Importer}") String applicationName
    ) {
        return new OpenAPI()
                .info(new Info()
                        .title("YugiBuilder Card Importer API")
                        .version("1.0.0")
                        .description("API pour l'importation des cartes Yu-Gi-Oh depuis YGOProDeck")
                        .contact(new Contact()
                                .name("YugiBuilder Dev Team")
                                .url("https://github.com/YugiBuilder")
                                .email("dev@yugibuilder.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Serveur de développement local"),
                        new Server()
                                .url("https://api.yugibuilder.com")
                                .description("Serveur de production")
                ));
    }
}
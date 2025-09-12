package com.yugibuilder.cardimporter.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // CORRECTION: Permet les mocks inutiles
@ActiveProfiles("test")
class RestTemplateConfigTest {

    private RestTemplateConfig restTemplateConfig;
    private RestTemplateBuilder mockBuilder;
    private RestTemplate mockRestTemplate;

    @BeforeEach
    void setup() {
        restTemplateConfig = new RestTemplateConfig();
        mockBuilder = mock(RestTemplateBuilder.class, RETURNS_SELF);
        mockRestTemplate = mock(RestTemplate.class);
        when(mockBuilder.build()).thenReturn(mockRestTemplate);
    }

    @Nested
    @DisplayName("Tests pour getApiBaseUrl - Méthode privée")
    class GetApiBaseUrlTest {

        @Test
        @DisplayName("getApiBaseUrl devrait retourner l'URL complète quand il n'y a pas de paramètres")
        void shouldReturnCompleteUrlWhenNoQueryParameters() {
            // Given
            String urlWithoutParams = "https://db.ygoprodeck.com/api/v7/cardinfo.php";
            ReflectionTestUtils.setField(restTemplateConfig, "apiUrl", urlWithoutParams);

            // When
            String result = ReflectionTestUtils.invokeMethod(restTemplateConfig, "getApiBaseUrl");

            // Then - CORRECTION: L'URL complète doit être retournée car il n'y a pas de "?"
            assertThat(result).isEqualTo(urlWithoutParams);
        }

        @Test
        @DisplayName("getApiBaseUrl devrait retourner la base URL quand il y a des paramètres")
        void shouldReturnBaseUrlWhenQueryParametersExist() {
            // Given
            String urlWithParams = "https://db.ygoprodeck.com/api/v7/cardinfo.php?type=Normal Monster&race=Dragon";
            ReflectionTestUtils.setField(restTemplateConfig, "apiUrl", urlWithParams);

            // When
            String result = ReflectionTestUtils.invokeMethod(restTemplateConfig, "getApiBaseUrl");

            // Then
            assertThat(result).isEqualTo("https://db.ygoprodeck.com/api/v7/cardinfo.php");
        }

        @Test
        @DisplayName("getApiBaseUrl devrait retourner l'URL par défaut quand apiUrl est null")
        void shouldReturnDefaultUrlWhenApiUrlIsNull() {
            // Given
            ReflectionTestUtils.setField(restTemplateConfig, "apiUrl", null);

            // When
            String result = ReflectionTestUtils.invokeMethod(restTemplateConfig, "getApiBaseUrl");

            // Then
            assertThat(result).isEqualTo("https://db.ygoprodeck.com/api/v7");
        }

        @Test
        @DisplayName("getApiBaseUrl devrait retourner l'URL par défaut quand apiUrl est une chaîne vide")
        void shouldReturnDefaultUrlWhenApiUrlIsEmpty() {
            // Given
            ReflectionTestUtils.setField(restTemplateConfig, "apiUrl", "");

            // When
            String result = ReflectionTestUtils.invokeMethod(restTemplateConfig, "getApiBaseUrl");

            // Then
            assertThat(result).isEqualTo("https://db.ygoprodeck.com/api/v7");
        }

        @Test
        @DisplayName("getApiBaseUrl devrait gérer les URLs avec un seul paramètre")
        void shouldHandleUrlWithSingleParameter() {
            // Given
            String urlWithSingleParam = "https://db.ygoprodeck.com/api/v7/cardinfo.php?race=Dragon";
            ReflectionTestUtils.setField(restTemplateConfig, "apiUrl", urlWithSingleParam);

            // When
            String result = ReflectionTestUtils.invokeMethod(restTemplateConfig, "getApiBaseUrl");

            // Then
            assertThat(result).isEqualTo("https://db.ygoprodeck.com/api/v7/cardinfo.php");
        }

        @Test
        @DisplayName("getApiBaseUrl devrait gérer les URLs malformées")
        void shouldHandleMalformedUrls() {
            // Given - URL avec plusieurs points d'interrogation
            String malformedUrl = "https://api.test.com/path?param1=value1?param2=value2";
            ReflectionTestUtils.setField(restTemplateConfig, "apiUrl", malformedUrl);

            // When
            String result = ReflectionTestUtils.invokeMethod(restTemplateConfig, "getApiBaseUrl");

            // Then - Devrait prendre tout jusqu'au premier ?
            assertThat(result).isEqualTo("https://api.test.com/path");
        }
    }

    @Nested
    @DisplayName("Tests d'intégration pour RestTemplate Bean")
    class RestTemplateBeanTest {

        @Test
        @DisplayName("restTemplate bean devrait être configuré avec les timeouts corrects")
        void shouldConfigureRestTemplateWithCorrectTimeouts() {
            // Given
            String testUrl = "https://db.ygoprodeck.com/api/v7/cardinfo.php?test=param";
            ReflectionTestUtils.setField(restTemplateConfig, "apiUrl", testUrl);

            // When
            RestTemplate result = restTemplateConfig.restTemplate(mockBuilder);

            // Then
            assertThat(result).isEqualTo(mockRestTemplate);

            // Vérifications des appels avec RETURNS_SELF
            verify(mockBuilder).rootUri("https://db.ygoprodeck.com/api/v7/cardinfo.php");
            verify(mockBuilder).connectTimeout(Duration.ofSeconds(30));
            verify(mockBuilder).readTimeout(Duration.ofSeconds(60));
            verify(mockBuilder).build();
        }

        @Test
        @DisplayName("restTemplate bean devrait utiliser l'URL par défaut quand apiUrl est null")
        void shouldUseDefaultUrlWhenApiUrlIsNull() {
            // Given
            ReflectionTestUtils.setField(restTemplateConfig, "apiUrl", null);

            // When
            restTemplateConfig.restTemplate(mockBuilder);

            // Then
            verify(mockBuilder).rootUri("https://db.ygoprodeck.com/api/v7");
        }

        @Test
        @DisplayName("restTemplate bean devrait gérer une URL complète sans paramètres")
        void shouldHandleCompleteUrlWithoutParameters() {
            // Given - CORRECTION: Test pour URL complète sans paramètres
            String completeUrl = "https://db.ygoprodeck.com/api/v7/cardinfo.php";
            ReflectionTestUtils.setField(restTemplateConfig, "apiUrl", completeUrl);

            // When
            restTemplateConfig.restTemplate(mockBuilder);

            // Then - L'URL complète doit être utilisée
            verify(mockBuilder).rootUri(completeUrl);
        }

        @Test
        @DisplayName("restTemplate bean devrait être créé correctement")
        void shouldCreateRestTemplateBean() {
            // Given
            ReflectionTestUtils.setField(restTemplateConfig, "apiUrl",
                    "https://db.ygoprodeck.com/api/v7/cardinfo.php");

            // When
            RestTemplate result = restTemplateConfig.restTemplate(mockBuilder);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(mockRestTemplate);
        }
    }
}
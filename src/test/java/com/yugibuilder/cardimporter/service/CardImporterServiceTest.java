package com.yugibuilder.cardimporter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yugibuilder.cardimporter.config.YgoProDeckProperties;
import com.yugibuilder.cardimporter.repository.CardImageRepository;
import com.yugibuilder.cardimporter.repository.CardSetRepository;
import com.yugibuilder.cardimporter.repository.YugiohCardRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class CardImporterServiceTest {

    @Mock
    private YugiohCardRepository cardRepository;

    @Mock
    private CardImageRepository imageRepository;

    @Mock
    private CardSetRepository setRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private YgoProDeckProperties properties;

    private CardImporterService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        service = new CardImporterService(
                cardRepository, imageRepository, setRepository,
                properties, objectMapper, restTemplate
        );
    }

    @Test
    void shouldImportCardsWhenApiReturnsValidData() {
        // Given
        when(properties.getUrl()).thenReturn("http://test.api.com");

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("data", createTestCardsData());

        when(restTemplate.getForEntity(any(String.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));

        when(cardRepository.saveAll(any())).thenReturn(List.of());

        // When
        int result = service.importAllCards();

        // Then
        assertThat(result).isEqualTo(1);
    }

    @Test
    void shouldThrowExceptionWhenApiReturnsError() {
        // Given
        when(properties.getUrl()).thenReturn("http://test.api.com");
        when(restTemplate.getForEntity(any(String.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR));

        // When & Then
        assertThatThrownBy(() -> service.importAllCards())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Importation échouée");
    }

    private List<Map<String, Object>> createTestCardsData() {
        Map<String, Object> card = new HashMap<>();
        card.put("id", 12345);
        card.put("name", "Test Card");
        card.put("type", "Normal Monster");
        card.put("desc", "A test card");
        card.put("atk", 1000);
        card.put("def", 1000);
        card.put("level", 4);
        card.put("race", "Warrior");
        card.put("attribute", "EARTH");

        return List.of(card);
    }
}
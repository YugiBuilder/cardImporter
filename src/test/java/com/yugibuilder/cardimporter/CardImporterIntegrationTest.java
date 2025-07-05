package com.yugibuilder.cardimporter;

import com.yugibuilder.cardimporter.model.YugiohCard;
import com.yugibuilder.cardimporter.repository.YugiohCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CardImporterIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private YugiohCardRepository cardRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @BeforeEach
    void setup() {
        cardRepository.deleteAll(); // Clean DB
    }

    @Test
    void shouldImportCardsAndStoreThemInMongoDB() {
        String url = "http://localhost:" + port + "/cards";

        // Exécution du POST /cards
        String result = restTemplate.postForObject(url, null, String.class);

        // Vérifie la réponse
        assertThat(result).contains("✅ Import terminé");

        // Vérifie que des cartes sont présentes en base
        List<YugiohCard> cards = cardRepository.findAll();
        assertThat(cards.size()).isGreaterThan(0);
    }
}
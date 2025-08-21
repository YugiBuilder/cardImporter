package com.yugibuilder.cardimporter;

import com.yugibuilder.cardimporter.config.RestTemplateConfig;
import com.yugibuilder.cardimporter.dto.CardWithSetAndImageDTO;
import com.yugibuilder.cardimporter.model.CardImage;
import com.yugibuilder.cardimporter.model.CardSet;
import com.yugibuilder.cardimporter.model.YugiohCard;
import com.yugibuilder.cardimporter.repository.CardImageRepository;
import com.yugibuilder.cardimporter.repository.CardSetRepository;
import com.yugibuilder.cardimporter.repository.YugiohCardRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureDataMongo
@Import(RestTemplateConfig.class)
class CardImporterIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private YugiohCardRepository cardRepository;

    @Autowired
    private CardImageRepository imageRepository;

    @Autowired
    private CardSetRepository setRepository;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private RestTemplate restTemplate; // Pour le mock

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setup() {
        // Nettoyer la base de données avant chaque test
        cardRepository.deleteAll();
        imageRepository.deleteAll();
        setRepository.deleteAll();

        // ✅ Utiliser la configuration adaptée pour rootUri
        mockServer = MockRestServiceServer.bindTo(restTemplate)
                .ignoreExpectOrder(true)  // Permet les appels asynchrones
                .build();

        // ✅ Pattern plus permissif
        mockServer.expect(ExpectedCount.manyTimes(),
                        requestTo(containsString("ygoprodeck.com")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        createDetailedMockYgoProDeckResponse(),
                        MediaType.APPLICATION_JSON
                ));
    }

    @AfterEach
    void cleanup() {
        if (mockServer != null) {
            mockServer.reset();
        }
    }

    @Test
    void shouldReturnHealthStatus() {
        // When
        String url = "http://localhost:" + port + "/cards/health";
        ResponseEntity<String> response = testRestTemplate.getForEntity(url, String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("API Card Importer fonctionnelle");
    }

    @Test
    void shouldImportCardsSuccessfully() {
        String mockApiResponse = createDetailedMockYgoProDeckResponse();

        // Matcher l’URL complète
        mockServer.expect(requestTo("https://db.ygoprodeck.com/api/v7/cardinfo.php"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(mockApiResponse, MediaType.APPLICATION_JSON));

        String url = "http://localhost:" + port + "/cards";
        ResponseEntity<String> response = testRestTemplate.postForEntity(url, null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Import terminé");
        assertThat(response.getBody()).contains("2 cartes enregistrées");

        verifyDataPersistence();
    }

    @Test
    void shouldRetrieveCardBySetName() {
        // Given - Insérer des données de test
        insertTestData();

        // When
        String url = "http://localhost:" + port + "/cards/by-set/Legend of Blue Eyes White Dragon";
        ResponseEntity<CardWithSetAndImageDTO> response = testRestTemplate.getForEntity(
                url, CardWithSetAndImageDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        CardWithSetAndImageDTO result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.getYugiohCard().getName()).isEqualTo("Dark Magician");
        assertThat(result.getCardSet().getSet_name()).isEqualTo("Legend of Blue Eyes White Dragon");
        assertThat(result.getCardImage().getImage_url()).contains("46986414.jpg");
    }

    @Test
    void shouldReturnNotFoundForUnknownSet() {
        // When
        String url = "http://localhost:" + port + "/cards/by-set/UnknownSet";
        ResponseEntity<String> response = testRestTemplate.getForEntity(url, String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldHandleMultipleCardsImport() {
        // Given – Mock de la réponse API avec 3 cartes
        String mockResponse = createMultipleCardsMockResponse();

        mockServer.reset();  // réinitialise toutes les attentes précédentes

        mockServer.expect(requestTo(startsWith("https://db.ygoprodeck.com/api/v7/cardinfo.php")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        // When – Appel de l’endpoint d’import
        String url = "http://localhost:" + port + "/cards";
        ResponseEntity<String> response = testRestTemplate.postForEntity(url, null, String.class);

        // Then – Vérifications
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("3 cartes enregistrées");

        // Vérifier les données en base
        List<YugiohCard> cards = cardRepository.findAll();
        assertThat(cards).hasSize(3);
        List<CardImage> images = imageRepository.findAll();
        assertThat(images).hasSizeGreaterThanOrEqualTo(3);
        List<CardSet> sets = setRepository.findAll();
        assertThat(sets).hasSizeGreaterThanOrEqualTo(3);

        mockServer.verify();
    }

    // ========== MÉTHODES UTILITAIRES ==========

    private void verifyDataPersistence() {
        // Vérifier les cartes
        List<YugiohCard> cards = cardRepository.findAll();
        assertThat(cards).hasSize(2);

        YugiohCard darkMagician = cards.stream()
                .filter(card -> "Dark Magician".equals(card.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Dark Magician not found"));

        assertThat(darkMagician.getAtk()).isEqualTo(2500);
        assertThat(darkMagician.getDef()).isEqualTo(2100);
        assertThat(darkMagician.getLevel()).isEqualTo(7);
        assertThat(darkMagician.getRace()).isEqualTo("Spellcaster");
        assertThat(darkMagician.getAttribute()).isEqualTo("DARK");

        // Vérifier les images
        List<CardImage> images = imageRepository.findAll();
        assertThat(images).hasSizeGreaterThanOrEqualTo(2);

        // Vérifier les sets
        List<CardSet> sets = setRepository.findAll();
        assertThat(sets).hasSizeGreaterThanOrEqualTo(2);

        // Vérifier les relations
        assertThat(darkMagician.getCardImageIds()).isNotEmpty();
        assertThat(darkMagician.getCardSetIds()).isNotEmpty();
    }

    private void insertTestData() {
        // Créer et sauver un set
        CardSet testSet = new CardSet();
        testSet.setSet_name("Legend of Blue Eyes White Dragon");
        testSet.setSet_code("LOB-005");
        testSet.setSet_rarity("Ultra Rare");
        testSet.setSet_price("291.56");
        testSet = setRepository.save(testSet);

        // Créer et sauver une image
        CardImage testImage = new CardImage();
        testImage.setImage_id(46986414);
        testImage.setImage_url("https://images.ygoprodeck.com/images/cards/46986414.jpg");
        testImage.setImage_url_small("https://images.ygoprodeck.com/images/cards_small/46986414.jpg");
        testImage = imageRepository.save(testImage);

        // Créer et sauver une carte
        YugiohCard testCard = new YugiohCard();
        testCard.setId(46986414);
        testCard.setName("Dark Magician");
        testCard.setType("Normal Monster");
        testCard.setDesc("The ultimate wizard in terms of attack and defense.");
        testCard.setAtk(2500);
        testCard.setDef(2100);
        testCard.setLevel(7);
        testCard.setRace("Spellcaster");
        testCard.setAttribute("DARK");
        testCard.setArchetype("Dark Magician");
        testCard.setCardImageIds(List.of(testImage.getId()));
        testCard.setCardSetIds(List.of(testSet.getId()));

        cardRepository.save(testCard);
    }

    private String createDetailedMockYgoProDeckResponse() {
        return """
            {
                "data": [
                    {
                        "id": 46986414,
                        "name": "Dark Magician",
                        "type": "Normal Monster",
                        "desc": "The ultimate wizard in terms of attack and defense.",
                        "atk": 2500,
                        "def": 2100,
                        "level": 7,
                        "race": "Spellcaster",
                        "attribute": "DARK",
                        "archetype": "Dark Magician",
                        "card_sets": [
                            {
                                "set_name": "Legend of Blue Eyes White Dragon",
                                "set_code": "LOB-005",
                                "set_rarity": "Ultra Rare",
                                "set_price": "291.56"
                            }
                        ],
                        "card_images": [
                            {
                                "id": 46986414,
                                "image_url": "https://images.ygoprodeck.com/images/cards/46986414.jpg",
                                "image_url_small": "https://images.ygoprodeck.com/images/cards_small/46986414.jpg"
                            }
                        ]
                    },
                    {
                        "id": 89631139,
                        "name": "Blue-Eyes White Dragon",
                        "type": "Normal Monster",
                        "desc": "This legendary dragon is a powerful engine of destruction.",
                        "atk": 3000,
                        "def": 2500,
                        "level": 8,
                        "race": "Dragon",
                        "attribute": "LIGHT",
                        "archetype": "Blue-Eyes",
                        "card_sets": [
                            {
                                "set_name": "Legend of Blue Eyes White Dragon",
                                "set_code": "LOB-001",
                                "set_rarity": "Ultra Rare",
                                "set_price": "350.00"
                            }
                        ],
                        "card_images": [
                            {
                                "id": 89631139,
                                "image_url": "https://images.ygoprodeck.com/images/cards/89631139.jpg",
                                "image_url_small": "https://images.ygoprodeck.com/images/cards_small/89631139.jpg"
                            }
                        ]
                    }
                ]
            }
            """;
    }

    private String createMultipleCardsMockResponse() {
        return """
            {
                "data": [
                    {
                        "id": 12345,
                        "name": "Test Card 1",
                        "type": "Effect Monster",
                        "desc": "A test card.",
                        "atk": 1000,
                        "def": 1000,
                        "level": 4,
                        "race": "Warrior",
                        "attribute": "EARTH",
                        "card_sets": [
                            {
                                "set_name": "Test Set 1",
                                "set_code": "TST-001",
                                "set_rarity": "Common",
                                "set_price": "1.00"
                            }
                        ],
                        "card_images": [
                            {
                                "id": 12345,
                                "image_url": "https://test.com/12345.jpg",
                                "image_url_small": "https://test.com/small/12345.jpg"
                            }
                        ]
                    },
                    {
                        "id": 67890,
                        "name": "Test Card 2",
                        "type": "Spell Card",
                        "desc": "Another test card.",
                        "card_sets": [
                            {
                                "set_name": "Test Set 2",
                                "set_code": "TST-002",
                                "set_rarity": "Rare",
                                "set_price": "5.00"
                            }
                        ],
                        "card_images": [
                            {
                                "id": 67890,
                                "image_url": "https://test.com/67890.jpg",
                                "image_url_small": "https://test.com/small/67890.jpg"
                            }
                        ]
                    },
                    {
                        "id": 11111,
                        "name": "Test Card 3",
                        "type": "Trap Card",
                        "desc": "Yet another test card.",
                        "card_sets": [
                            {
                                "set_name": "Test Set 3",
                                "set_code": "TST-003",
                                "set_rarity": "Super Rare",
                                "set_price": "10.00"
                            }
                        ],
                        "card_images": [
                            {
                                "id": 11111,
                                "image_url": "https://test.com/11111.jpg",
                                "image_url_small": "https://test.com/small/11111.jpg"
                            }
                        ]
                    }
                ]
            }
            """;
    }
}
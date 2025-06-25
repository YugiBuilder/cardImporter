package com.yugibuilder.cardimporter.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yugibuilder.cardimporter.config.YgoProDeckProperties;
import com.yugibuilder.cardimporter.model.YugiohCard;
import com.yugibuilder.cardimporter.repository.YugiohCardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class CardImporterService {

    private static final Logger logger = LoggerFactory.getLogger(CardImporterService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final YugiohCardRepository repository;
    private final YgoProDeckProperties properties;
    private final ObjectMapper objectMapper;

    @Autowired
    public CardImporterService(YugiohCardRepository repository, YgoProDeckProperties properties, ObjectMapper objectMapper) {
        this.repository = repository;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Importe toutes les cartes depuis l'API YgoProDeck.
     * Cette méthode récupère les données de l'API, les convertit en entités YugiohCard,
     * et les enregistre dans la base de données.
     *
     * @return Le nombre de cartes importées ou mises à jour.
     */
    public int importAllCards() {
        try {
            logger.info("📥 Importation des cartes depuis l'API : {}", properties.getUrl());

            ResponseEntity<Map> response = restTemplate.getForEntity(properties.getUrl(), Map.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                logger.error("❌ Erreur API : {}", response.getStatusCode());
                return 0;
            }

            Object data = response.getBody().get("data");
            List<Map<String, Object>> cards = objectMapper.convertValue(
                    data,
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            if (cards == null) return 0;

            List<YugiohCard> cardEntities = new ArrayList<>();

            for (Map<String, Object> item : cards) {
                Integer cardId = item.get("id") != null ? ((Number) item.get("id")).intValue() : null;
                if (cardId == null) continue;

                Optional<YugiohCard> existingCardOpt = repository.findById(cardId);
                YugiohCard card = existingCardOpt.orElse(new YugiohCard());
                card.setId(cardId);

                card.setName((String) item.get("name"));
                card.setType((String) item.get("type"));
                card.setDesc((String) item.get("desc"));
                card.setAtk(item.get("atk") != null ? ((Number) item.get("atk")).intValue() : null);
                card.setDef(item.get("def") != null ? ((Number) item.get("def")).intValue() : null);
                card.setLevel(item.get("level") != null ? ((Number) item.get("level")).intValue() : null);
                card.setRace((String) item.get("race"));
                card.setAttribute((String) item.get("attribute"));
                card.setArchetype((String) item.get("archetype"));

                card.setCard_images(objectMapper.convertValue(
                        item.get("card_images"),
                        new TypeReference<List<Map<String, Object>>>() {}));

                card.setCard_sets(objectMapper.convertValue(
                        item.get("card_sets"),
                        new TypeReference<List<Map<String, Object>>>() {}));

                card.setCard_prices(objectMapper.convertValue(
                        item.get("card_prices"),
                        new TypeReference<List<Map<String, Object>>>() {}));

                cardEntities.add(card);
            }

            repository.saveAll(cardEntities);
            logger.info("✅ Importation terminée : {} cartes enregistrées ou mises à jour.", cardEntities.size());
            return cardEntities.size();

        } catch (RestClientException e) {
            logger.error("🚨 Erreur lors de l’appel API", e);
            return 0;
        } catch (Exception e) {
            logger.error("🔥 Erreur inattendue", e);
            return 0;
        }
    }

    /**
     * Tâche planifiée pour importer les cartes toutes les 2 semaines.
     * Cette méthode est exécutée par le planificateur de tâches de Spring.
     */
    @Scheduled(cron = "0 0 4 * * FRI", zone = "Europe/Paris") // Tous les vendredis à 4h00
    public void scheduledImport() {
        if (isEvenWeek()) {
            logger.info("🔄 Début de l'importation planifiée des cartes.");
            importAllCards();
            logger.info("🔄 Importation planifiée terminée. Nombre de cartes importées : {}", repository.count());
        } else {
            logger.info("⏸️ Pas d'importation cette semaine. Seules les semaines paires sont traitées.");
        }
    }

    /**
     * Tâche planifiée pour importer les cartes toutes les 2 semaines.
     * Cette méthode est exécutée par le planificateur de tâches de Spring.
     */
    private boolean isEvenWeek() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.temporal.WeekFields weekFields = java.time.temporal.WeekFields.ISO;
        int weekNumber = today.get(weekFields.weekOfWeekBasedYear());
        return weekNumber % 2 == 0;
    }
}
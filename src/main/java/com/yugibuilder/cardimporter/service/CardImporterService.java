package com.yugibuilder.cardimporter.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yugibuilder.cardimporter.config.YgoProDeckProperties;
import com.yugibuilder.cardimporter.model.CardImage;
import com.yugibuilder.cardimporter.model.CardSet;
import com.yugibuilder.cardimporter.model.YugiohCard;
import com.yugibuilder.cardimporter.repository.CardImageRepository;
import com.yugibuilder.cardimporter.repository.CardSetRepository;
import com.yugibuilder.cardimporter.repository.YugiohCardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class CardImporterService {

    private static final Logger logger = LoggerFactory.getLogger(CardImporterService.class);

    private final YugiohCardRepository cardRepository;
    private final CardImageRepository imageRepository;
    private final CardSetRepository setRepository;
    private final YgoProDeckProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Autowired
    public CardImporterService(
            YugiohCardRepository cardRepository,
            CardImageRepository imageRepository,
            CardSetRepository setRepository,
            YgoProDeckProperties properties,
            ObjectMapper objectMapper,
            RestTemplate restTemplate
    ) {
        this.cardRepository = cardRepository;
        this.imageRepository = imageRepository;
        this.setRepository = setRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    /**
     * Importe toutes les cartes depuis l'API YgoProDeck.
     * Cette méthode récupère les données de l'API, les convertit en entités,
     * et les enregistre dans la base de données.
     *
     * @return Le nombre de cartes importées.
     */
    public int importAllCards() {
        try {
            logger.info("📥 Importation depuis {}", properties.getUrl());

            ResponseEntity<Map> response = restTemplate.getForEntity(properties.getUrl(), Map.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                logger.error("❌ Erreur API : {}", response.getStatusCode());
                throw new RuntimeException("Erreur API");
            }

            Object data = response.getBody().get("data");
            List<Map<String, Object>> cards = objectMapper.convertValue(
                    data,
                    new TypeReference<>() {}
            );

            List<YugiohCard> entities = new ArrayList<>();
            for (Map<String, Object> item : cards) {
                Integer id = item.get("id") != null ? ((Number) item.get("id")).intValue() : null;

                if (id == null) continue;

                YugiohCard card = cardRepository.findById(id).orElse(new YugiohCard());
                card.setId(id);
                card.setName((String) item.get("name"));
                card.setType((String) item.get("type"));
                card.setDesc((String) item.get("desc"));
                card.setAtk(convertInt(item.get("atk")));
                card.setDef(convertInt(item.get("def")));
                card.setLevel(convertInt(item.get("level")));
                card.setRace((String) item.get("race"));
                card.setAttribute((String) item.get("attribute"));
                card.setArchetype((String) item.get("archetype"));

                // Images
                List<Map<String, Object>> images = (List<Map<String, Object>>) item.get("card_images");
                List<String> imageIds = new ArrayList<>();
                if (images != null) {
                    for (Map<String, Object> img : images) {
                        CardImage image = objectMapper.convertValue(img, CardImage.class);
                        image = imageRepository.save(image);
                        imageIds.add(image.getId());
                    }
                }
                card.setCardImageIds(imageIds);

                // Sets
                List<Map<String, Object>> sets = (List<Map<String, Object>>) item.get("card_sets");
                List<String> setIds = new ArrayList<>();
                if (sets != null) {
                    for (Map<String, Object> set : sets) {
                        CardSet cardSet = objectMapper.convertValue(set, CardSet.class);
                        cardSet = setRepository.save(cardSet);
                        setIds.add(cardSet.getId());
                    }
                }
                card.setCardSetIds(setIds);

                entities.add(card);
            }

            cardRepository.saveAll(entities);
            logger.info("✅ Importation réussie : {} cartes", entities.size());
            return entities.size();

        } catch (Exception e) {
            logger.error("🔥 Erreur durant l’import", e);
            throw new RuntimeException("Importation échouée", e);
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
            logger.info("🔄 Importation planifiée terminée. Nombre de cartes importées : {}", cardRepository.count());
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

    /**
     * Convertit un objet en Integer.
     * Si l'objet est une instance de Number, il est converti en Integer.
     * Si l'objet est null ou n'est pas un Number, retourne null.
     * @param value L'objet à convertir.
     * @return L'Integer converti ou null si la conversion échoue.
     */
    private Integer convertInt(Object value) {
        return (value instanceof Number) ? ((Number) value).intValue() : null;
    }
}
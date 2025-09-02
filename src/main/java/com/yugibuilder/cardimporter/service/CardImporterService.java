package com.yugibuilder.cardimporter.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yugibuilder.cardimporter.config.YgoProDeckProperties;
import com.yugibuilder.cardimporter.dto.CardWithDetailsDTO;
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
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
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
                    data, new TypeReference<List<Map<String, Object>>>() {}
            );

            Map<String, CardSet> setCache = new HashMap<>();
            setRepository.findAll().forEach(set -> {
                String key = set.getSet_name() + "||" + set.getSet_code();
                setCache.put(key, set);
            });

            List<CardImage> imagesToSave = new ArrayList<>();
            List<CardSet> setsToSave = new ArrayList<>();
            List<YugiohCard> cardsToSave = new ArrayList<>();

            int batchSize = 1000;
            int totalCards = cards.size();

            for (int i = 0; i < totalCards; i += batchSize) {
                int endIndex = Math.min(i + batchSize, totalCards);
                List<Map<String, Object>> batch = cards.subList(i, endIndex);

                logger.info("📦 Traitement du batch {}-{}/{}", i + 1, endIndex, totalCards);

                processBatch(batch, setCache, imagesToSave, setsToSave, cardsToSave);

                saveBatch(imagesToSave, setsToSave, cardsToSave);

                imagesToSave.clear();
                setsToSave.clear();
                cardsToSave.clear();
            }

            logger.info("✅ Importation réussie : {} cartes", totalCards);
            return totalCards;

        } catch (Exception e) {
            logger.error("🔥 Erreur durant l'import", e);
            throw new RuntimeException("Importation échouée", e);
        }
    }

    private void processBatch(List<Map<String, Object>> batch, Map<String, CardSet> setCache,
                              List<CardImage> imagesToSave, List<CardSet> setsToSave,
                              List<YugiohCard> cardsToSave) {

        for (Map<String, Object> item : batch) {
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
                    imagesToSave.add(image);
                    imageIds.add(image.getId());
                }
            }
            card.setCardImageIds(imageIds);

            // Sets (utilisation du set_code comme référence)
            List<Map<String, Object>> sets = (List<Map<String, Object>>) item.get("card_sets");
            List<String> setCodes = new ArrayList<>();
            if (sets != null) {
                for (Map<String, Object> set : sets) {
                    String setName = (String) set.get("set_name");
                    String setCode = (String) set.get("set_code");
                    String cacheKey = setName + "||" + setCode;

                    CardSet cardSet = setCache.get(cacheKey);
                    if (cardSet == null) {
                        cardSet = objectMapper.convertValue(set, CardSet.class);
                        setsToSave.add(cardSet);
                        setCache.put(cacheKey, cardSet);
                    }
                    setCodes.add(setCode);
                }
            }
            card.setCardSetCodes(setCodes);
            cardsToSave.add(card);
        }
    }

    private void saveBatch(List<CardImage> imagesToSave, List<CardSet> setsToSave,
                           List<YugiohCard> cardsToSave) {
        if (!imagesToSave.isEmpty()) {
            imageRepository.saveAll(imagesToSave);
            logger.debug("💾 Sauvegardé {} images", imagesToSave.size());
        }

        if (!setsToSave.isEmpty()) {
            setRepository.saveAll(setsToSave);
            logger.debug("💾 Sauvegardé {} sets", setsToSave.size());
        }

        if (!cardsToSave.isEmpty()) {
            cardRepository.saveAll(cardsToSave);
            logger.debug("💾 Sauvegardé {} cartes", cardsToSave.size());
        }
    }

    @Scheduled(cron = "0 0 4 * * FRI", zone = "Europe/Paris")
    public void scheduledImport() {
        if (isEvenWeek()) {
            logger.info("🔄 Début de l'importation planifiée des cartes.");
            importAllCards();
            logger.info("🔄 Importation planifiée terminée. Nombre de cartes importées : {}", cardRepository.count());
        } else {
            logger.info("⏸️ Pas d'importation cette semaine. Seules les semaines paires sont traitées.");
        }
    }

    private boolean isEvenWeek() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.temporal.WeekFields weekFields = java.time.temporal.WeekFields.ISO;
        int weekNumber = today.get(weekFields.weekOfWeekBasedYear());
        return weekNumber % 2 == 0;
    }

    private Integer convertInt(Object value) {
        return (value instanceof Number) ? ((Number) value).intValue() : null;
    }

    public List<CardWithDetailsDTO> getCardsWithDetailsBySetName(String setName) {
        List<CardSet> cardSets = setRepository.findAllBySetNameIgnoreCase(setName);

        if (cardSets.isEmpty()) {
            logger.warn("Aucun set trouvé pour le nom : {}", setName);
            return Collections.emptyList();
        }

        List<String> setCodes = cardSets.stream()
                .map(CardSet::getSet_code)
                .toList();

        List<YugiohCard> allCards = cardRepository.findAll();
        List<CardWithDetailsDTO> cardsWithDetails = new ArrayList<>();

        for (YugiohCard card : allCards) {
            if (card.getCardSetCodes() != null) {
                boolean belongsToSet = card.getCardSetCodes().stream()
                        .anyMatch(setCodes::contains);

                if (belongsToSet) {
                    CardSet matchingSet = cardSets.stream()
                            .filter(set -> card.getCardSetCodes().contains(set.getSet_code()))
                            .findFirst()
                            .orElse(null);

                    CardImage cardImage = null;
                    if (card.getCardImageIds() != null && !card.getCardImageIds().isEmpty()) {
                        String imageId = card.getCardImageIds().get(0);
                        cardImage = imageRepository.findById(imageId).orElse(null);
                    }

                    CardWithDetailsDTO dto = new CardWithDetailsDTO(card, matchingSet, cardImage);
                    cardsWithDetails.add(dto);
                }
            }
        }

        logger.info("Nombre de cartes trouvées dans les sets {} : {}", setName, cardsWithDetails.size());
        return cardsWithDetails;
    }
}

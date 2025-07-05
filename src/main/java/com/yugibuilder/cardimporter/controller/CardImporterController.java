package com.yugibuilder.cardimporter.controller;

import com.yugibuilder.cardimporter.dto.CardWithSetAndImageDTO;
import com.yugibuilder.cardimporter.model.CardImage;
import com.yugibuilder.cardimporter.model.CardSet;
import com.yugibuilder.cardimporter.model.YugiohCard;
import com.yugibuilder.cardimporter.repository.CardImageRepository;
import com.yugibuilder.cardimporter.repository.CardSetRepository;
import com.yugibuilder.cardimporter.repository.YugiohCardRepository;
import com.yugibuilder.cardimporter.service.CardImporterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/cards")
@Tags(value = @Tag(name = "Card Importer", description = "Endpoints for importing Yu-Gi-Oh cards"))
public class CardImporterController {
    private final CardImporterService cardImporterService;
    private final CardSetRepository cardSetRepository;
    private final YugiohCardRepository yugiohCardRepository;
    private final CardImageRepository cardImageRepository;


    @Autowired
    public CardImporterController(CardImporterService cardImporterService, CardSetRepository cardSetRepository,
                                  YugiohCardRepository yugiohCardRepository, CardImageRepository cardImageRepository) {
        this.cardImporterService = cardImporterService;
        this.cardSetRepository = cardSetRepository;
        this.yugiohCardRepository = yugiohCardRepository;
        this.cardImageRepository = cardImageRepository;
    }

    /**
     * Endpoint to import all Yu-Gi-Oh cards from the YGOProDeck API.
     * This will fetch the latest card data and store it in the database.
     *
     * @return ResponseEntity with a message indicating the number of cards imported.
     */
    @Operation(summary = "Import all Yu-Gi-Oh cards",
            description = "Fetches and imports all cards from the YGOProDeck API into the database."
    , tags = {"Card Importer"})
    @PostMapping
    public ResponseEntity<String> importCards() {
        int count = cardImporterService.importAllCards();
        return ResponseEntity.ok("✅ Import terminé : " + count + " cartes enregistrées.");
    }

    /**
     * Endpoint to get a card by its set name.
     * This will return the card, its set, and the associated image.
     *
     * @param setName The name of the card set.
     * @return ResponseEntity with CardWithSetAndImageDTO if found, or 404 Not Found if not.
     */
    @GetMapping("/by-set/{setName}")
    @Operation(summary = "Get card by set", description = "Retourne une carte, son set et son image associée pour un nom de set donné")
    public ResponseEntity<CardWithSetAndImageDTO> getCardBySet(@PathVariable String setName) {
        Optional<CardSet> cardSetOpt = cardSetRepository.findAll().stream()
                .filter(s -> s.getSet_name().equalsIgnoreCase(setName))
                .findFirst();

        if (cardSetOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CardSet targetSet = cardSetOpt.get();

        Optional<YugiohCard> cardOpt = yugiohCardRepository.findAll().stream()
                .filter(card -> card.getCardSetIds() != null && card.getCardSetIds().contains(targetSet.getId()))
                .findFirst();

        if (cardOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        YugiohCard card = cardOpt.get();

        int setIndex = card.getCardSetIds().indexOf(targetSet.getId());
        String imageId = (card.getCardImageIds() != null && setIndex < card.getCardImageIds().size())
                ? card.getCardImageIds().get(setIndex)
                : null;

        CardImage image = (imageId != null) ? cardImageRepository.findById(imageId).orElse(null) : null;

        CardWithSetAndImageDTO dto = new CardWithSetAndImageDTO();
        dto.setYugiohCard(card);
        dto.setCardSet(targetSet);
        dto.setCardImage(image);

        return ResponseEntity.ok(dto);
    }
}

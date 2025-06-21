package com.yugibuilder.cardimporter.controller;

import com.yugibuilder.cardimporter.service.CardImporterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cards")
@Tags(value = @Tag(name = "Card Importer", description = "Endpoints for importing Yu-Gi-Oh cards"))
public class CardImporterController {
    private final CardImporterService cardImporterService;

    @Autowired
    public CardImporterController(CardImporterService cardImporterService) {
        this.cardImporterService = cardImporterService;
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
}

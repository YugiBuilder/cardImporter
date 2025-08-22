package com.yugibuilder.cardimporter.controller;

import com.yugibuilder.cardimporter.dto.CardWithSetAndImageDTO;
import com.yugibuilder.cardimporter.model.CardImage;
import com.yugibuilder.cardimporter.model.CardSet;
import com.yugibuilder.cardimporter.model.YugiohCard;
import com.yugibuilder.cardimporter.repository.CardImageRepository;
import com.yugibuilder.cardimporter.repository.CardSetRepository;
import com.yugibuilder.cardimporter.repository.YugiohCardRepository;
import com.yugibuilder.cardimporter.service.CardImporterService;

// FIXED: Updated imports for OpenAPI 3 (Spring Boot 3.4)
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/cards")
@Tag(name = "Card Importer", description = "Endpoints pour l'importation et la gestion des cartes Yu-Gi-Oh")
public class CardImporterController {

    private final CardImporterService cardImporterService;
    private final CardSetRepository cardSetRepository;
    private final YugiohCardRepository yugiohCardRepository;
    private final CardImageRepository cardImageRepository;

    @Autowired
    public CardImporterController(
            CardImporterService cardImporterService,
            CardSetRepository cardSetRepository,
            YugiohCardRepository yugiohCardRepository,
            CardImageRepository cardImageRepository
    ) {
        this.cardImporterService = cardImporterService;
        this.cardSetRepository = cardSetRepository;
        this.yugiohCardRepository = yugiohCardRepository;
        this.cardImageRepository = cardImageRepository;
    }

    /**
     * Endpoint pour importer toutes les cartes Yu-Gi-Oh depuis l'API YGOProDeck.
     * Cette méthode récupère les dernières données de cartes et les stocke en base.
     *
     * @return ResponseEntity avec un message indiquant le nombre de cartes importées.
     */
    @Operation(
            summary = "Importer toutes les cartes Yu-Gi-Oh",
            description = "Récupère et importe toutes les cartes depuis l'API YGOProDeck dans la base de données"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Import réussi",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erreur lors de l'import",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PostMapping
    public ResponseEntity<String> importCards() {
        int count = cardImporterService.importAllCards();
        return ResponseEntity.ok("✅ Import terminé : " + count + " cartes enregistrées.");
    }

    /**
     * Endpoint pour récupérer une carte par son nom de set.
     * Retourne la carte, son set et l'image associée.
     *
     * @param setName Le nom du set de cartes.
     * @return ResponseEntity avec CardWithSetAndImageDTO si trouvé, ou 404 Not Found sinon.
     */
    @GetMapping("/by-set/{setName}")
    @Operation(
            summary = "Récupérer une carte par nom de set",
            description = "Retourne une carte, son set et son image associée pour un nom de set donné"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Carte trouvée",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CardWithSetAndImageDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Carte non trouvée",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<List<YugiohCard>> getCardBySet(
            @Parameter(description = "Nom du set de cartes", required = true, example = "Blue-Eyes White Dragon")
            @PathVariable String setName
    ) {
        List<YugiohCard> cards = cardImporterService.getCardsBySetName(setName);

        if (cards.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(cards);
    }

    /**
     * Endpoint pour obtenir le statut de santé de l'API
     */
    @GetMapping("/health")
    @Operation(
            summary = "Vérifier le statut de l'API",
            description = "Endpoint de santé pour vérifier que l'API fonctionne correctement"
    )
    @ApiResponse(
            responseCode = "200",
            description = "API fonctionnelle",
            content = @Content(mediaType = "application/json")
    )
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("🟢 API Card Importer fonctionnelle");
    }
}
package com.yugibuilder.cardimporter.controller;

import com.yugibuilder.cardimporter.dto.CardWithDetailsDTO;
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

@RestController
@RequestMapping("/cards")
@Tag(name = "Card Importer", description = "Endpoints pour l'importation et la gestion des cartes Yu-Gi-Oh")
public class CardImporterController {

    private final CardImporterService cardImporterService;

    @Autowired
    public CardImporterController(
            CardImporterService cardImporterService
    ) {
        this.cardImporterService = cardImporterService;
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
            summary = "Récupérer des cartes par nom de set",
            description = "Retourne toutes les cartes, leurs sets et images associées pour un nom de set donné. Retourne une liste vide si aucune carte n'est trouvée."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Requête réussie - peut retourner une liste vide si aucune carte n'est trouvée",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CardWithDetailsDTO.class)
                    )
            )
    })
    public ResponseEntity<List<CardWithDetailsDTO>> getCardBySet(
            @Parameter(description = "Nom du set de cartes", required = true, example = "Legend of Blue Eyes White Dragon")
            @PathVariable String setName
    ) {
        List<CardWithDetailsDTO> cards = cardImporterService.getCardsWithDetailsBySetName(setName);

        // ✅ CORRECTION : Toujours retourner 200 OK avec la liste (vide ou non)
        // Une liste vide est une réponse valide, pas une erreur 404
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
package com.yugibuilder.cardimporter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.yugibuilder.cardimporter.dto.CardWithDetailsDTO;
import com.yugibuilder.cardimporter.model.CardImage;
import com.yugibuilder.cardimporter.model.CardSet;
import com.yugibuilder.cardimporter.model.YugiohCard;
import com.yugibuilder.cardimporter.repository.CardImageRepository;
import com.yugibuilder.cardimporter.repository.CardSetRepository;
import com.yugibuilder.cardimporter.repository.YugiohCardRepository;
import com.yugibuilder.cardimporter.service.CardImporterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;


import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CardImporterController.class)
@ActiveProfiles("test")
class CardImporterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CardImporterService service;

    @MockitoBean
    private CardSetRepository cardSetRepository;

    @MockitoBean
    private YugiohCardRepository yugiohCardRepository;

    @MockitoBean
    private CardImageRepository cardImageRepository;

    @Test
    @DisplayName("POST /cards → 200 avec nombre de cartes importées")
    void importCardsSuccess() throws Exception {
        given(service.importAllCards()).willReturn(5);

        mockMvc.perform(post("/cards"))
                .andExpect(status().isOk())
                .andExpect(content().string("✅ Import terminé : 5 cartes enregistrées."));

        verifyNoInteractions(cardSetRepository, yugiohCardRepository, cardImageRepository);
    }

    @Test
    @DisplayName("POST /cards → 500 quand le service lance une exception")
    void importCardsFailure() throws Exception {
        given(service.importAllCards()).willThrow(new RuntimeException("Erreur API"));

        mockMvc.perform(post("/cards"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("GET /cards/health → 200 OK")
    void healthCheck() throws Exception {
        mockMvc.perform(get("/cards/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("🟢 API Card Importer fonctionnelle"));
        // service et repos ne doivent pas être appelés
        verifyNoInteractions(service, cardSetRepository, yugiohCardRepository, cardImageRepository);
    }

    @Test
    @DisplayName("GET /cards/by-set/{setName} → 200 avec DTO complet")
    void getCardBySetFound() throws Exception {
        // Préparer les entités et DTO
        CardSet set = new CardSet();
        set.setId("s1");
        set.setSet_name("MySet");

        YugiohCard card = new YugiohCard();
        card.setId(1);
        card.setName("CardA");
        card.setCardSetCodes(List.of("s1"));
        card.setCardImageIds(List.of("i1"));

        CardImage image = new CardImage();
        image.setImage_id(1);
        image.setImage_url("url1");

        // Mock du service au lieu des repositories
        CardWithDetailsDTO expectedDto = new CardWithDetailsDTO(card, set, image);
        given(service.getCardsWithDetailsBySetName("MySet"))
                .willReturn(List.of(expectedDto));

        mockMvc.perform(get("/cards/by-set/{setName}", "MySet")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))  // Vérifier qu'on a une liste avec 1 élément
                .andExpect(jsonPath("$[0].yugiohCard.name").value("CardA"))
                .andExpect(jsonPath("$[0].cardSet.set_name").value("MySet"))
                .andExpect(jsonPath("$[0].cardImage.image_url").value("url1"));
    }


    @Test
    @DisplayName("GET /cards/by-set/{setName} → 200 avec liste vide quand le set est introuvable")
    void getCardBySetNotFoundSet() throws Exception {
        // ✅ CORRECTION : Le service retourne une liste vide au lieu d'une exception
        given(service.getCardsWithDetailsBySetName("NoSet"))
                .willReturn(Collections.emptyList());

        mockMvc.perform(get("/cards/by-set/{setName}", "NoSet")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // ✅ CORRECTION : 200 au lieu de 404
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0))); // ✅ Liste vide
    }

    @Test
    @DisplayName("GET /cards/by-set/{setName} → 200 avec liste vide quand aucune carte pour le set")
    void getCardBySetNotFoundCard() throws Exception {
        // ✅ CORRECTION : Le service retourne une liste vide au lieu d'une exception
        given(service.getCardsWithDetailsBySetName("MySet"))
                .willReturn(Collections.emptyList());

        mockMvc.perform(get("/cards/by-set/{setName}", "MySet")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // ✅ CORRECTION : 200 au lieu de 404
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0))); // ✅ Liste vide
    }
}

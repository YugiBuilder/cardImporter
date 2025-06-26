package com.yugibuilder.cardimporter.controller;

import com.yugibuilder.cardimporter.service.CardImporterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CardImporterController.class)
public class CardImporterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardImporterService service;

    @Test
    void shouldReturnSuccessWhenImportSucceeds() throws Exception {
        when(service.importAllCards()).thenReturn(42);

        mockMvc.perform(post("/cards"))
                .andExpect(status().isOk())
                .andExpect(content().string("✅ Import terminé : 42 cartes enregistrées."));
    }

    @Test
    void shouldReturnErrorWhenServiceFails() throws Exception {
        when(service.importAllCards()).thenThrow(new RuntimeException("Erreur API"));

        mockMvc.perform(post("/cards"))
                .andExpect(status().is5xxServerError());
    }
}

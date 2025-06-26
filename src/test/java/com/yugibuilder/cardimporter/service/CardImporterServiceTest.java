package com.yugibuilder.cardimporter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yugibuilder.cardimporter.config.YgoProDeckProperties;
import com.yugibuilder.cardimporter.repository.YugiohCardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CardImporterServiceTest {

    @Mock
    private YugiohCardRepository repository;

    @Mock
    private YgoProDeckProperties properties;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CardImporterService service;

    @Test
    void shouldHandleInvalidApiUrlGracefully() {
        when(properties.getUrl()).thenReturn("http://invalid-url");

        service.importAllCards(); // Ne doit pas planter même si l'URL est invalide

        verifyNoInteractions(repository);
    }
}

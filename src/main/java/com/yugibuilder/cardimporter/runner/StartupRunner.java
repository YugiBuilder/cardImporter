package com.yugibuilder.cardimporter.runner;

import com.yugibuilder.cardimporter.service.CardImporterService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupRunner implements CommandLineRunner {

    private final CardImporterService importerService;

    public StartupRunner(CardImporterService importerService) {
        this.importerService = importerService;
    }

    @Override
    public void run(String... args) {
        importerService.importAllCards();
    }
}
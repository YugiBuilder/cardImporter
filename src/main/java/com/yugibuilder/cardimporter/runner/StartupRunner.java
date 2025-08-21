package com.yugibuilder.cardimporter.runner;

import com.yugibuilder.cardimporter.service.CardImporterService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
// FIXED: Only run when specifically enabled via property
@ConditionalOnProperty(
        name = "app.import-cards-on-startup",
        havingValue = "true",
        matchIfMissing = false
)
public class StartupRunner implements CommandLineRunner {

    private final CardImporterService importerService;

    public StartupRunner(CardImporterService importerService) {
        this.importerService = importerService;
    }

    @Override
    public void run(String... args) {
        try {
            // Add a small delay to ensure MongoDB is fully ready
            Thread.sleep(2000);
            importerService.importAllCards();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for MongoDB", e);
        }
    }
}
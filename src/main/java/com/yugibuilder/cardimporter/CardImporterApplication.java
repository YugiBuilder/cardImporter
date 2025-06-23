package com.yugibuilder.cardimporter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CardImporterApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardImporterApplication.class, args);
    }

}

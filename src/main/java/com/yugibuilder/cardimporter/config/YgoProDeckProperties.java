package com.yugibuilder.cardimporter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ygoprodeck.api")
@Setter
@Getter
public class YgoProDeckProperties {
    private String url;
}

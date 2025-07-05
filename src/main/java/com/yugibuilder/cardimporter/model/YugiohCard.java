package com.yugibuilder.cardimporter.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Document(collection = "cards")
@Getter
@Setter
public class YugiohCard {
    @Id
    private Integer id;
    private String name;
    private String type;
    private String desc;
    private Integer atk;
    private Integer def;
    private Integer level;
    private String race;
    private String attribute;
    private String archetype;

    // Liste des ID des images de la carte
    private List<String> cardImageIds;

    // Liste des ID des sets de la carte
    private List<String> cardSetIds;

    // Prix par plateforme
    private List<Map<String, Object>> card_prices;
}

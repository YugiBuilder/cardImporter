package com.yugibuilder.cardimporter.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "card_sets")
public class CardSet {
    @Id
    private String id;

    private String set_name;
    private String set_code;
    private String set_rarity;
    private String set_price;
    private Integer cardId; // référence vers la carte
}
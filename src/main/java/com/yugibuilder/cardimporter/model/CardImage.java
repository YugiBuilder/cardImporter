package com.yugibuilder.cardimporter.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "card_images")
public class CardImage {
    @Id
    private String id; // ✅ Gardez String pour MongoDB
    private Integer image_id; // ✅ L'ID YgoProDeck
    private String image_url;
    private String image_url_small;
}

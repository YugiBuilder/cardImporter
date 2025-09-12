package com.yugibuilder.cardimporter.dto;

import com.yugibuilder.cardimporter.model.CardImage;
import com.yugibuilder.cardimporter.model.CardSet;
import com.yugibuilder.cardimporter.model.YugiohCard;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class CardWithDetailsDTO {
    private YugiohCard yugiohCard;
    private CardSet cardSet;
    private CardImage cardImage;

    public CardWithDetailsDTO(YugiohCard yugiohCard, CardSet cardSet, CardImage cardImage) {
        this.yugiohCard = yugiohCard;
        this.cardSet = cardSet;
        this.cardImage = cardImage;
    }
}

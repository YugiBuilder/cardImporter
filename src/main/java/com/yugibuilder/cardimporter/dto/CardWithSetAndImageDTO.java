package com.yugibuilder.cardimporter.dto;

import com.yugibuilder.cardimporter.model.CardImage;
import com.yugibuilder.cardimporter.model.CardSet;
import com.yugibuilder.cardimporter.model.YugiohCard;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class CardWithSetAndImageDTO {
    private YugiohCard yugiohCard;
    private CardSet cardSet;
    private CardImage cardImage;
}

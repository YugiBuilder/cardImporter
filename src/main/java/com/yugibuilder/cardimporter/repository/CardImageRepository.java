package com.yugibuilder.cardimporter.repository;

import com.yugibuilder.cardimporter.model.CardImage;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CardImageRepository extends MongoRepository<CardImage,String> {
}

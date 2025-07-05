package com.yugibuilder.cardimporter.repository;

import com.yugibuilder.cardimporter.model.CardSet;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CardSetRepository extends MongoRepository<CardSet,String> {
}

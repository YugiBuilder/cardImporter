package com.yugibuilder.cardimporter.repository;

import com.yugibuilder.cardimporter.model.YugiohCard;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface YugiohCardRepository extends MongoRepository<YugiohCard, Integer> {
}

package com.yugibuilder.cardimporter.repository;

import com.yugibuilder.cardimporter.model.YugiohCard;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface YugiohCardRepository extends MongoRepository<YugiohCard, Integer> {
    @Query("{ 'cardSetIds': ?0 }")
    List<YugiohCard> findAllByCardSetIdsContains(String setId);
}

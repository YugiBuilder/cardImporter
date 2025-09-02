package com.yugibuilder.cardimporter.repository;

import com.yugibuilder.cardimporter.model.CardSet;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CardSetRepository extends MongoRepository<CardSet,String> {
    // Nouvelle méthode qui retourne une liste
    @Query("{ 'set_name' : { $regex: ?0, $options: 'i' } }")
    List<CardSet> findAllBySetNameIgnoreCase(String set_name);
    @Query("{ 'set_name' : { $regex: ?0, $options: 'i' } }")
    Optional<CardSet> findBySetNameIgnoreCase(String set_name);
}

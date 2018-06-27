package com.hackdays.aws.transformer.road.repositories;

import com.hackdays.aws.transformer.road.domains.StreetConfidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StreetConfidenceRepository extends JpaRepository<StreetConfidence, Integer> {
    List<StreetConfidence> findByScoreLessThanEqual(Float score);
}

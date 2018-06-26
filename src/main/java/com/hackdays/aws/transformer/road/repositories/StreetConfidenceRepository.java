package com.hackdays.aws.transformer.road.repositories;

import com.hackdays.aws.transformer.road.domains.StreetConfidence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreetConfidenceRepository extends JpaRepository<StreetConfidence, Long> {
}

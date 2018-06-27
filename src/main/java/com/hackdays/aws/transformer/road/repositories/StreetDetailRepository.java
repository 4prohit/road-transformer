package com.hackdays.aws.transformer.road.repositories;

import com.hackdays.aws.transformer.road.domains.StreetDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreetDetailRepository extends JpaRepository<StreetDetail, Integer> {
}

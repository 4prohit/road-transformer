package com.hackdays.aws.transformer.road.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.hackdays.aws.transformer.road.domains.Subscription;

public interface SubscriptionRepository extends JpaRepository<Subscription, Integer> {
	List<Subscription> findByLocationId(Integer locationId);
}

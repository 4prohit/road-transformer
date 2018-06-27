package com.hackdays.aws.transformer.road.services;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hackdays.aws.transformer.road.domains.StreetDetail;
import com.hackdays.aws.transformer.road.domains.Subscription;
import com.hackdays.aws.transformer.road.repositories.SubscriptionRepository;


@Service
public class NotificationService {
	
	private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
	
	SubscriptionRepository subscriptionRepository;
	
	@Value( "${email-notifications.alert-confidence}" )
	private float alertConfidence;
	
	public NotificationService(SubscriptionRepository subscriptionRepository) {
		this.subscriptionRepository = subscriptionRepository;
	}
	
	public void checkAndSendNotifications(StreetDetail streetDetail, float confidence) {
		if (confidence < alertConfidence) {
			logger.info(String.format("Confidence level of %s is bad, preparing to send emails...", confidence));
			int locationId = streetDetail.getLocationId();
			List<Subscription> subscriptions = subscriptionRepository.findByLocationId(locationId);
			if (subscriptions.size() > 0) {
				for (Subscription subscription: subscriptions) {
					EmailService.sendBadRoadEmail(subscription.getEmail(), streetDetail, confidence);
				}
			} else {
				logger.info(String.format("Nobody is subscribing to location ID: %d", locationId));
			}
		} else {
			logger.info(String.format("Confidence level of %s is good!", confidence));
		}
		
	}

}

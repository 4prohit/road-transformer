package com.hackdays.aws.transformer.road.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hackdays.aws.transformer.road.domains.StreetDetail;


@Service
public class NotificationService {
	
	private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
	
	Map<Integer, Set<String>> subscriptionList; // Map of road id to emails
	
	@Value( "${email-notifications.alert-confidence}" )
	private float alertConfidence;
	
	public NotificationService() {
		this.subscriptionList = new HashMap<>();
		Set<String> emailSet = new HashSet<String>(Arrays.asList("gohhuimay@gmail.com"));
		subscriptionList.put(100, emailSet);
	}
	
	public void checkAndSendNotifications(StreetDetail streetDetail, float confidence) {
		if (confidence < alertConfidence) {
			logger.info(String.format("Confidence level of %s is bad, preparing to send emails...", confidence));
			int roadId = streetDetail.getRoadId();
			if(subscriptionList.containsKey(roadId)) {
				for (String email: subscriptionList.get(roadId)) {
					EmailService.sendBadRoadEmail(email, streetDetail, confidence);
				}
			} else {
				logger.info(String.format("Nobody is subscribing to road ID: %d", roadId));
			}
		} else {
			logger.info(String.format("Confidence level of %s is good!", confidence));
		}
		
	}

}

package com.hackdays.aws.transformer.road.controllers;

import com.hackdays.aws.transformer.road.domains.StreetDetail;
import com.hackdays.aws.transformer.road.domains.Subscription;
import com.hackdays.aws.transformer.road.exceptions.ImageNotSuitableException;
import com.hackdays.aws.transformer.road.exceptions.RoadNotFoundException;
import com.hackdays.aws.transformer.road.repositories.SubscriptionRepository;
import com.hackdays.aws.transformer.road.services.NotificationService;
import com.hackdays.aws.transformer.road.services.RoadService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

//@Validated
@RestController
public class RoadController {

    private static final Logger logger = LoggerFactory.getLogger(RoadController.class);

    private RoadService roadService;
    private NotificationService notificationService;
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    public RoadController(RoadService roadService, NotificationService notificationService, SubscriptionRepository subscriptionRepository) {
        this.roadService = roadService;
        this.notificationService = notificationService;
        this.subscriptionRepository = subscriptionRepository;
    }

    @PostMapping(value = "/v1/upload", consumes = {"multipart/form-data"}, produces = "application/json")
    public ResponseEntity<?> upload(@RequestParam("locationId") Integer locationId, @RequestParam("image") MultipartFile multipartFile) {
        if (multipartFile.isEmpty()) {
            return new ResponseEntity<>("Empty file", HttpStatus.BAD_REQUEST);
        }
        logger.info("Received file with {} bytes", multipartFile.getSize());
        logger.info("Processing the file for road confidence...");
        try {
            return new ResponseEntity<>(roadService.processImage(multipartFile, locationId), HttpStatus.OK);
        } catch (RoadNotFoundException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "Failed");
            result.put("message", "Invalid location! No road found for given location.");
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (ImageNotSuitableException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "Failed");
            result.put("message", "Image not is suitable! No labels detected for road or street!");
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "Failed");
            result.put("message", "Failed to analyze the image.");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
    }

    @GetMapping(value = "/v1/streetDetails", produces = "application/json")
    public ResponseEntity<?> getStreetDetails() {
        try {
            return new ResponseEntity<>(roadService.getStreetDetails(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "/v1/streetConfidence", produces = "application/json")
    public ResponseEntity<?> getStreetConfidence(@RequestParam(value = "confidence", required = false, defaultValue = "60") Integer confidence) {
        try {
            return new ResponseEntity<>(roadService.getStreetConfidence(confidence), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/subscribe")
    public ResponseEntity<?> createSubscription(Subscription subscription) {
        try {
            Subscription savedSubscription = subscriptionRepository.save(subscription);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String responseMsg = String.format("%s has successfully subscribed to %s", subscription.getEmail(), subscription.getLocationId());
        return new ResponseEntity<>(responseMsg, HttpStatus.CREATED);
    }

    @GetMapping("/testSendEmail")
    public ResponseEntity<?> sendEmail(@RequestParam(value = "locationId", defaultValue = "1") int locationId,
                                       @RequestParam(value = "roadName", defaultValue = "Seletar Expressway") String roadName,
                                       @RequestParam(value = "country", defaultValue = "Singapore") String country,
                                       @RequestParam(value = "roadConfidence", defaultValue = "30.5") float roadConfidence,
                                       @RequestParam(value = "streetConfidence", defaultValue = "10.4") float streetConfidence) {
        final Set<String> ROAD_LABELS = new HashSet<>(Arrays.asList("road", "street"));
        final Map<String, Float> labelConfidence = new HashMap<>();
        StreetDetail streetDetail = new StreetDetail();
        streetDetail.setLocationId(locationId);
        streetDetail.setCountry(country);
        streetDetail.setRoadName(roadName);
        labelConfidence.put("road", roadConfidence);
        labelConfidence.put("street", streetConfidence);
        float totalConfidence = 0;
        int count = 0;
        for (Map.Entry<String, Float> confidenceMap : labelConfidence.entrySet()) {
            if (ROAD_LABELS.contains(confidenceMap.getKey())) {
                totalConfidence += confidenceMap.getValue();
                count += 1;
            }
        }
        float avgConfidence = totalConfidence / count;
        notificationService.checkAndSendNotifications(streetDetail, avgConfidence);
        String responseString = String.format("Notification sent for %s. \nAverage confidence of %s", streetDetail.toString(), avgConfidence);
        return new ResponseEntity<>(responseString, HttpStatus.OK);
    }
}

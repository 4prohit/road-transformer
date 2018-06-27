package com.hackdays.aws.transformer.road.controllers;

import com.hackdays.aws.transformer.road.exceptions.ImageNotSuitableException;
import com.hackdays.aws.transformer.road.exceptions.RoadNotFoundException;
import com.hackdays.aws.transformer.road.services.RoadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

//@Validated
@RestController
public class RoadController {

    private static final Logger logger = LoggerFactory.getLogger(RoadController.class);

    private RoadService roadService;

    @Autowired
    public RoadController(RoadService roadService) {
        this.roadService = roadService;
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
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ImageNotSuitableException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

package com.hackdays.aws.transformer.road.controllers;

import com.hackdays.aws.transformer.road.dto.ImageDetailDTO;
import com.hackdays.aws.transformer.road.services.ImageProcessingService;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

//@Validated
@RequestMapping("/v1")
@RestController
public class RekonController {

    private static final Logger logger = LoggerFactory.getLogger(RekonController.class);

    private ImageProcessingService imageProcessingService;

    @Autowired
    public RekonController(ImageProcessingService imageProcessingService) {
        this.imageProcessingService = imageProcessingService;
    }

    @ApiOperation(value = "Upload Image", notes = "Upload image, analyze with AWS Rekognition and store in AWS S3")
    @PostMapping(value = "/upload", consumes = {"multipart/form-data"}, produces = "application/json")
    public ResponseEntity<?> upload(@RequestPart(value = "details", required = false) ImageDetailDTO imageDetailDTO, @RequestPart("image") MultipartFile multipartFile) {
        if (multipartFile.isEmpty()) {
            return new ResponseEntity<>("Empty file", HttpStatus.BAD_REQUEST);
        }
        logger.info("Received file with {} bytes", multipartFile.getSize());
        logger.info("Processing the file for road confidence...");
        try {
            return new ResponseEntity<>(imageProcessingService.processImage(multipartFile, imageDetailDTO), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

package com.hackdays.aws.transformer.road.services;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.util.IOUtils;
import com.hackdays.aws.transformer.road.domains.StreetConfidence;
import com.hackdays.aws.transformer.road.domains.StreetDetail;
import com.hackdays.aws.transformer.road.exceptions.ImageNotSuitableException;
import com.hackdays.aws.transformer.road.exceptions.NoLabelsException;
import com.hackdays.aws.transformer.road.exceptions.RoadNotFoundException;
import com.hackdays.aws.transformer.road.exceptions.S3BucketException;
import com.hackdays.aws.transformer.road.repositories.StreetConfidenceRepository;
import com.hackdays.aws.transformer.road.repositories.StreetDetailRepository;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class RoadService {

    private static final Logger logger = LoggerFactory.getLogger(RoadService.class);

    private StreetDetailRepository streetDetailRepository;

    private StreetConfidenceRepository streetConfidenceRepository;

    private AmazonRekognition amazonRekognition;

    private AmazonS3 amazonS3;

    private String bucketName;
    
    private NotificationService notificationService;

    private static final String ROAD = "road";
    private static final String BLANK = "";
    private static final Set<String> ROAD_LABELS = new HashSet<>(Arrays.asList("road", "street"));

    @Autowired
    public RoadService(StreetDetailRepository streetDetailRepository, StreetConfidenceRepository streetConfidenceRepository, AmazonRekognition amazonRekognition, AmazonS3 amazonS3, @Value("${aws.s3.bucketName}") String bucketName, NotificationService notificationService) {
        this.streetDetailRepository = streetDetailRepository;
        this.streetConfidenceRepository = streetConfidenceRepository;
        this.amazonRekognition = amazonRekognition;
        this.amazonS3 = amazonS3;
        this.bucketName = bucketName;
        this.notificationService = notificationService;
    }

    public List<StreetDetail> getStreetDetails() {
        return streetDetailRepository.findAll();
    }

    public Map<String, Object> processImage(MultipartFile multipartFile, Integer locationId) throws NoLabelsException, S3BucketException, IOException, ImageNotSuitableException, RoadNotFoundException {

        Optional<StreetDetail> streetDetailById = streetDetailRepository.findById(locationId);

        if (!streetDetailById.isPresent()) {
            throw new RoadNotFoundException("No road found for given locationId");
        }
        StreetDetail streetDetail = streetDetailById.get();

        logger.info("Detecting labels in the image...");
        DetectLabelsRequest detectLabelsRequest = new DetectLabelsRequest();
        detectLabelsRequest.setImage(getImageUtil(multipartFile));
        DetectLabelsResult detectLabelsResult = amazonRekognition.detectLabels(detectLabelsRequest);
        if (null == detectLabelsResult) {
            throw new NoLabelsException("Failed to detect labels in the image");
        }

        logger.info("Retrieving confidence for the labels...");
        List<Label> labels = detectLabelsResult.getLabels();
        if (CollectionUtils.isEmpty(labels)) {
            throw new NoLabelsException("No labels detected in the image");
        }
        final Map<String, Float> labelConfidence = new HashMap<>();
        labels.forEach(label -> labelConfidence.put(label.getName().toLowerCase(), label.getConfidence()));
        if (Collections.disjoint(labelConfidence.keySet(), ROAD_LABELS)) {
            throw new ImageNotSuitableException("Image is not suitable for road confidence");
        }
        
        float totalConfidence = 0;
        int count = 0;
        for (Map.Entry<String, Float> confidenceMap: labelConfidence.entrySet()) {
        	if (ROAD_LABELS.contains(confidenceMap.getKey())) {
        		totalConfidence += confidenceMap.getValue();
        		count += 1;
        	}
        }
        float avgConfidence = totalConfidence/count;
        notificationService.checkAndSendNotifications(streetDetail, avgConfidence);

        if (!amazonS3.doesBucketExistV2(bucketName)) {
            logger.info("Bucket {} does not exists. Creating new bucket...", bucketName);
            try {
                amazonS3.createBucket(bucketName);
            } catch (AmazonS3Exception e) {
                throw new S3BucketException("Failed to create S3 bucket");
            }
        }

        logger.info("Putting image in S3 bucket...");
        File file = convertMultiPartToFile(multipartFile);
        String s3FilePath = String.format("%s_%s_%s.%s", streetDetail.getRoadId(), streetDetail.getCctvId(), new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime()), FilenameUtils.getExtension(file.getName()));
        if (0 < file.length()) {
            try {
                amazonS3.putObject(bucketName, s3FilePath, file);
            } catch (AmazonServiceException e) {
                throw new S3BucketException("Failed to put image in S3 bucket");
            } finally {
                if (file.delete()) {
                    logger.info("Temporary file deleted.");
                }
            }
        }

        StreetConfidence streetConfidence = saveStreetConfidence(labelConfidence, s3FilePath, streetDetail);

        Map<String, Object> result = new HashMap<>();
        result.put("score", streetConfidence.getScore());

        return result;
    }

    private StreetConfidence saveStreetConfidence(Map<String, Float> labelConfidence, String s3FilePath, StreetDetail streetDetail) {
        logger.info("Saving confidence score from the image...");
        StreetConfidence streetConfidence = new StreetConfidence();
        streetConfidence.setStreetDetail(streetDetail);
        streetConfidence.setScore(labelConfidence.get(ROAD));
        streetConfidence.setLabels(labelConfidence);
        streetConfidence.setS3FilePath(s3FilePath);
        streetConfidence.setUploadedBy(BLANK);
        streetConfidence.setCreatedOn(System.currentTimeMillis());
        streetConfidence = streetConfidenceRepository.save(streetConfidence);
        return streetConfidence;
    }

    private Image getImageUtil(MultipartFile uploadedImage) throws IOException {
        ByteBuffer imageBytes;
        try (InputStream inputStream = uploadedImage.getInputStream()) {
            imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
        }
        return new Image().withBytes(imageBytes);
    }

    private File convertMultiPartToFile(MultipartFile multipartFile) throws IOException {
        File file = new File(multipartFile.getOriginalFilename());
        try (OutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(multipartFile.getBytes());
        }
        return file;
    }
}

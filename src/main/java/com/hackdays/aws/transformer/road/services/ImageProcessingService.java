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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackdays.aws.transformer.road.domains.StreetConfidence;
import com.hackdays.aws.transformer.road.domains.StreetDetail;
import com.hackdays.aws.transformer.road.dto.ImageDetailDTO;
import com.hackdays.aws.transformer.road.exceptions.ImageNotSuitableException;
import com.hackdays.aws.transformer.road.exceptions.NoLabelsException;
import com.hackdays.aws.transformer.road.exceptions.S3BucketException;
import com.hackdays.aws.transformer.road.repositories.StreetConfidenceRepository;
import com.hackdays.aws.transformer.road.repositories.StreetDetailRepository;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ImageProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(ImageProcessingService.class);

    private StreetDetailRepository streetDetailRepository;

    private StreetConfidenceRepository streetConfidenceRepository;

    private AmazonRekognition amazonRekognition;

    private AmazonS3 amazonS3;

    private String bucketName;

    private String reverseGeocodingApi;

    private final ObjectMapper objectMapper;

    private static final String COMMA = ",";
    private static final String UNDERSCORE = "_";
    private static final String ROAD = "road";
    private static final String BLANK = "";
    private static final Set<String> ADDRESS_KEYS = new HashSet<>(Arrays.asList("route", "neighborhood", "locality", "country"));
    private static final Set<String> ROAD_LABELS = new HashSet<>(Arrays.asList("road", "street"));

    @Autowired
    public ImageProcessingService(StreetDetailRepository streetDetailRepository, StreetConfidenceRepository streetConfidenceRepository, AmazonRekognition amazonRekognition, AmazonS3 amazonS3, @Value("${aws.s3.bucketName}") String bucketName, @Value("${maps.reverse_geocoding_api}") String reverseGeocodingApi) {
        this.streetDetailRepository = streetDetailRepository;
        this.streetConfidenceRepository = streetConfidenceRepository;
        this.amazonRekognition = amazonRekognition;
        this.amazonS3 = amazonS3;
        this.bucketName = bucketName;
        this.reverseGeocodingApi = reverseGeocodingApi;
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public Map<String, Object> processImage(MultipartFile multipartFile, ImageDetailDTO imageDetailDTO) throws NoLabelsException, S3BucketException, IOException, ImageNotSuitableException {

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
        String s3FilePath = UUID.randomUUID().toString() + UNDERSCORE + new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime());
        if (null != file && 0 < file.length()) {
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

        StreetDetail streetDetail = getStreetDetail(imageDetailDTO);

        saveStreetDetail(streetDetail);

        StreetConfidence streetConfidence = saveStreetConfidence(labelConfidence, s3FilePath, streetDetail, imageDetailDTO);

        Map<String, Object> result = new HashMap<>();
        result.put("score", streetConfidence.getScore());

        return result;
    }

    private StreetConfidence saveStreetConfidence(Map<String, Float> labelConfidence, String s3FilePath, StreetDetail streetDetail, ImageDetailDTO imageDetailDTO) {
        logger.info("Saving confidence score from the image...");
        StreetConfidence streetConfidence = new StreetConfidence();
        streetConfidence.setStreetDetail(streetDetail);
        streetConfidence.setScore(labelConfidence.get(ROAD));
        streetConfidence.setLabels(labelConfidence);
        streetConfidence.setS3FilePath(s3FilePath);
        streetConfidence.setUploadedBy(BLANK);
        streetConfidence.setComments(imageDetailDTO.getComments());
        streetConfidence.setLatitude(imageDetailDTO.getLatitude());
        streetConfidence.setLongitude(imageDetailDTO.getLongitude());
        streetConfidence.setCreatedOn(System.currentTimeMillis());
        streetConfidence = streetConfidenceRepository.save(streetConfidence);
        return streetConfidence;
    }

    private void saveStreetDetail(StreetDetail streetDetail) {
        if (null != streetDetail) {
            String streetId = (streetDetail.getStreetName() + streetDetail.getNeighborhood() + streetDetail.getLocality() + streetDetail.getCountry()).replaceAll("\\s", BLANK);
            if (!streetDetailRepository.existsById(streetId)) {
                logger.info("Saving new street details...");
                streetDetail.setStreetId(streetId);
                streetDetail.setCreatedOn(System.currentTimeMillis());
                streetDetailRepository.save(streetDetail);
            }
        }
    }

    private StreetDetail getStreetDetail(ImageDetailDTO imageDetailDTO) {
        logger.info("Retrieving address from image location...");
        Map<String, String> addressMap = new HashMap<>();
        try {
            RestTemplate restTemplate = new RestTemplate();
            String finalUri = reverseGeocodingApi + imageDetailDTO.getLatitude() + COMMA + imageDetailDTO.getLongitude();
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(finalUri, String.class);
            if (!HttpStatus.OK.equals(responseEntity.getStatusCode()) || StringUtils.isEmpty(responseEntity.getBody())) {
                logger.error("Received null or invalid response from Geocoding API");
                return null;
            }
            JSONArray results = new JSONObject(responseEntity.getBody()).getJSONArray("results");
            if (null == results || 0 >= results.length()) {
                logger.error("Received 0 results from Geocoding API");
                return null;
            }
            JSONArray addressComponents = results.getJSONObject(0).getJSONArray("address_components");
            if (null == addressComponents || 0 >= addressComponents.length()) {
                logger.error("Address component is empty");
                return null;
            }
            for (int i = 0; i < addressComponents.length(); i++) {
                JSONObject addressComponent = addressComponents.getJSONObject(i);
                JSONArray types = addressComponent.getJSONArray("types");
                for (int j = 0; j < types.length(); j++) {
                    String type = types.getString(j).toLowerCase();
                    if (ADDRESS_KEYS.contains(type)) {
                        addressMap.put(type, addressComponent.getString("long_name"));
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get address of the image", e);
        }
        if (!CollectionUtils.isEmpty(addressMap)) {
            return objectMapper.convertValue(addressMap, StreetDetail.class);
        } else {
            return null;
        }
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

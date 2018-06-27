package com.hackdays.aws.transformer.road.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Value("${aws.accessKey}")
    private String accessKey;

    @Value("${aws.secretKey}")
    private String secretKey;

    @Bean
    public AWSCredentials awsCredentials() {
        return new BasicAWSCredentials(this.accessKey, this.secretKey);
    }

    @Bean
    public AmazonS3 amazonS3(AWSCredentials awsCredentials) {
        // @formatter:off
        return AmazonS3ClientBuilder.
                standard()
                .withRegion(Regions.US_EAST_1)
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
        // @formatter:on
    }

    @Bean
    public AmazonRekognition amazonRekognition(AWSCredentials awsCredentials) {
        // @formatter:off
        return AmazonRekognitionClientBuilder
                .standard()
                .withRegion(Regions.US_EAST_1)
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
        // @formatter:on
    }
}

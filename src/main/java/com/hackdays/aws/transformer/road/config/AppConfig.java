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
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
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

    @Bean
    public Docket customImplementation() {
        // @formatter:off
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("ccom.hackdays.aws.transformer.road"))
                .build()
                .apiInfo(apiInfo());
        // @formatter:on
    }

    private ApiInfo apiInfo() {
        // @formatter:off
        return new ApiInfoBuilder()
                .title("Road Transformer")
                .description("AWS Hackdays Singapore 2018")
                .version("1.0")
                .contact(new Contact("Rohit Patil", "", ""))
                .build();
        // @formatter:on
    }
}

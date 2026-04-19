package com.msb.upload.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Slf4j
@Configuration
public class MinioConfig {

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Value("${aws.s3.region:ap-southeast-1}")
    private String region;

    @Value("${aws.s3.endpoint:#{null}}")
    private String endpointOverride;

    // Dell EMC ECS (and other S3-compatible stores) require path-style URLs.
    // Set aws.s3.path-style-access=true in application.yml for ECS.
    @Value("${aws.s3.path-style-access:false}")
    private boolean pathStyleAccess;

    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKey, secretKey));
    }

    private S3Configuration s3Configuration() {
        return S3Configuration.builder()
            .pathStyleAccessEnabled(pathStyleAccess)
            .build();
    }

    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider())
            .serviceConfiguration(s3Configuration());
        if (endpointOverride != null && !endpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(endpointOverride));
        }
        log.info("Initializing S3 client: region={}, endpoint={}, pathStyle={}",
            region, endpointOverride != null ? endpointOverride : "AWS default", pathStyleAccess);
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        var builder = S3Presigner.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider())
            .serviceConfiguration(s3Configuration());
        if (endpointOverride != null && !endpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(endpointOverride));
        }
        return builder.build();
    }
}
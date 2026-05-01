package com.ttsham6.shared.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties({S3Property.class, ModelProperty.class})
public class S3Config {

  private final S3Property s3Property;

  public S3Config(S3Property s3Property) {
    this.s3Property = s3Property;
  }

  @Bean
  @Profile("local")
  public S3Client s3ClientLocal() {
    final var credentials =
        AwsBasicCredentials.create(s3Property.accessKey(), s3Property.secretKey());

    return S3Client.builder()
        .region(Region.AP_NORTHEAST_1)
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .build();
  }

  @Bean
  @Profile("prod")
  public S3Client s3ClientProd() {
    return S3Client.builder().region(Region.AP_NORTHEAST_1).build();
  }
}

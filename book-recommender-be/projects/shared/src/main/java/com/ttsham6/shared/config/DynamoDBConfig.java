package com.ttsham6.shared.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class DynamoDBConfig {

  private final String dynamoDbEndpointUrl;

  public DynamoDBConfig(
      @Value("${spring.cloud.aws.dynamodb.endpoint:}") String dynamoDbEndpointUrl) {
    this.dynamoDbEndpointUrl = dynamoDbEndpointUrl;
  }

  @Bean
  @Profile("shared-local")
  public DynamoDbClient localDynamoDbClient() {
    return DynamoDbClient.builder().endpointOverride(URI.create(dynamoDbEndpointUrl)).build();
  }

  @Bean
  @Profile("shared-prod")
  public DynamoDbClient prodDynamoDbClient() {
    return DynamoDbClient.builder().build();
  }

  @Bean
  public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
    return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
  }
}

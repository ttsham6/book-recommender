package com.ttsham6.shared.config;

import io.pinecone.clients.Pinecone;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PineconeConfig {

  private final String pineconeApiKey;

  public PineconeConfig(@Value("${book-recommender.pinecone.apikey}") String pineconeApiKey) {
    this.pineconeApiKey = pineconeApiKey;
  }

  @Bean
  public Pinecone pineconeClient() {
    return new Pinecone.Builder(pineconeApiKey).build();
  }
}

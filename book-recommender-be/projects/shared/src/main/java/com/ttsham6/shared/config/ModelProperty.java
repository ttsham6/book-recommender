package com.ttsham6.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "book-recommender.model")
public record ModelProperty(String dir, S3 s3) {

  public record S3(String bucket, String prefix) {

    public S3 {
      if (prefix == null) {
        prefix = "model/";
      }
    }
  }
}

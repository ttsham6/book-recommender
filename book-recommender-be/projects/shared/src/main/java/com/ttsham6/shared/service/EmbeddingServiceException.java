package com.ttsham6.shared.service;

public class EmbeddingServiceException extends Exception {
  public EmbeddingServiceException(String message) {
    super(message);
  }

  public EmbeddingServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}

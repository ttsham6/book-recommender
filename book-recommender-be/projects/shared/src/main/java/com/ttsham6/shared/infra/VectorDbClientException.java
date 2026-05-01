package com.ttsham6.shared.infra;

public class VectorDbClientException extends Exception {
  public VectorDbClientException(String message) {
    super(message);
  }

  public VectorDbClientException(String message, Throwable cause) {
    super(message, cause);
  }
}

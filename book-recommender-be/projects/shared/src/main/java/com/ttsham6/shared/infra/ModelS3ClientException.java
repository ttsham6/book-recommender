package com.ttsham6.shared.infra;

public class ModelS3ClientException extends Exception {
  public ModelS3ClientException(String message) {
    super(message);
  }

  public ModelS3ClientException(String message, Throwable cause) {
    super(message, cause);
  }
}

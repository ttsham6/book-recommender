package com.ttsham6.shared.external;

public class RakutenApiException extends Exception {
  public RakutenApiException(String message) {
    super(message);
  }

  public RakutenApiException(String message, Throwable cause) {
    super(message, cause);
  }
}

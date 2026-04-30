package com.ttsham6.shared.infrastructure;

public class DynamoDBRepositoryException extends Exception {
  public DynamoDBRepositoryException(String message) {
    super(message);
  }

  public DynamoDBRepositoryException(String message, Throwable cause) {}
}

package com.ttsham6.shared.infra;

import com.ttsham6.shared.infra.dto.BookDto;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

@Component
public class DynamoDBRepository {

  private static final Logger logger = LoggerFactory.getLogger(DynamoDBRepository.class);
  private final DynamoDbClient dynamoDbClient;
  private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
  private final String TABLE_NAME = "book";

  public DynamoDBRepository(
      DynamoDbClient dynamoDbClient, DynamoDbEnhancedClient dynamoDbEnhancedClient) {
    this.dynamoDbClient = dynamoDbClient;
    this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
  }

  private DynamoDbTable<BookDto> table() {
    return dynamoDbEnhancedClient.table(TABLE_NAME, TableSchema.fromImmutableClass(BookDto.class));
  }

  public void refresh() throws DynamoDBRepositoryException {
    final var waiter = dynamoDbClient.waiter();
    final var describeTableRequest = DescribeTableRequest.builder().tableName(TABLE_NAME).build();

    // テーブルが存在していたら削除
    try {
      table().deleteTable();
    } catch (ResourceNotFoundException e) {
      // テーブルが存在しない場合は何もしない
      logger.info("Table not found, nothing to delete.");
    } catch (Exception e) {
      throw new DynamoDBRepositoryException("Failed to delete table", e);
    }

    final var deleteWaitResponse = waiter.waitUntilTableNotExists(describeTableRequest);

    // テーブルを再作成
    try {
      table().createTable();
    } catch (Exception e) {
      throw new DynamoDBRepositoryException("Failed to create table", e);
    }

    final var _ignored = waiter.waitUntilTableExists(describeTableRequest);
  }

  public void createTable() {
    table().createTable();
  }

  public void deleteTable() {
    // テーブルが存在していたら削除
    try {
      table().deleteTable();
    } catch (ResourceNotFoundException e) {
      // テーブルが存在しない場合は何もしない
      logger.info("Table not found, nothing to delete.");
    }
  }

  public void save(BookDto bookDto) {
    final var table =
        dynamoDbEnhancedClient.table(TABLE_NAME, TableSchema.fromImmutableClass(BookDto.class));
    table.putItem(bookDto);
  }

  public List<BookDto> scan(List<String> words) throws DynamoDBRepositoryException {
    try {
      final var table = table();

      final var scanRequest =
          ScanEnhancedRequest.builder()
              .filterExpression(
                  Expression.builder()
                      .expression("contains(#name, :word)")
                      .putExpressionName("#name", "name")
                      .putExpressionValue(":word", AttributeValue.fromS(words.getFirst()))
                      .build())
              .build();

      final var results = table.scan(scanRequest);

      return results.items().stream().toList();
    } catch (DynamoDbException e) {
      throw new DynamoDBRepositoryException("Failed to scan clothing", e);
    }
  }

  public Optional<BookDto> findById(String id) {

    try {
      final var table = table();
      final var key = Key.builder().partitionValue(id).build();
      final var item = table.getItem(key);
      return Optional.ofNullable(item);
    } catch (DynamoDbException e) {
      logger.warn("Failed to find Book with id: {}", id, e);
      return Optional.empty();
    }
  }
}

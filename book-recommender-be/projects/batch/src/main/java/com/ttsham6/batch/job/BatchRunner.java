package com.ttsham6.batch.job;

import com.ttsham6.shared.domain.book.Book;
import com.ttsham6.shared.external.RakutenApiClient;
import com.ttsham6.shared.infrastructure.DynamoDBRepository;
import com.ttsham6.shared.infrastructure.DynamoDBRepositoryException;
import com.ttsham6.shared.infrastructure.dto.BookDto;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BatchRunner implements ApplicationRunner {

  private static final Logger logger = LoggerFactory.getLogger(BatchRunner.class);

  private final RakutenApiClient rakutenApiClient;
  private final DynamoDBRepository dynamoDBRepository;

  public BatchRunner(RakutenApiClient rakutenApiClient, DynamoDBRepository dynamoDBRepository) {
    this.rakutenApiClient = rakutenApiClient;
    this.dynamoDBRepository = dynamoDBRepository;
  }

  @Override
  public void run(ApplicationArguments args) {
    final var books = rakutenApiClient.streamBooks();
    updateDb(books);
  }

  private void updateDb(Stream<Book> books) {
    try {
      dynamoDBRepository.refresh();
    } catch (DynamoDBRepositoryException e) {
      logger.error("Failed to refresh clothing", e);
      throw new RuntimeException(e);
    }

    books.forEach(
        book -> {
          dynamoDBRepository.save(BookDto.from(book));
          // TODO: ベクトルDBも更新
        });
  }
}

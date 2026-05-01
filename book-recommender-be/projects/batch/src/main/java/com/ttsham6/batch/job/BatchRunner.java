package com.ttsham6.batch.job;

import com.ttsham6.shared.external.RakutenApiClient;
import com.ttsham6.shared.infra.DynamoDBRepository;
import com.ttsham6.shared.infra.DynamoDBRepositoryException;
import com.ttsham6.shared.infra.VectorDBRepository;
import com.ttsham6.shared.infra.VectorDbClientException;
import com.ttsham6.shared.infra.dto.BookDto;
import com.ttsham6.shared.service.EmbeddingServiceException;
import com.ttsham6.shared.service.LocalEmbeddingService;
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
  private final VectorDBRepository vectorDBRepository;
  private final LocalEmbeddingService embeddingService;

  public BatchRunner(
      RakutenApiClient rakutenApiClient,
      DynamoDBRepository dynamoDBRepository,
      VectorDBRepository vectorDBRepository,
      LocalEmbeddingService embeddingService) {
    this.rakutenApiClient = rakutenApiClient;
    this.dynamoDBRepository = dynamoDBRepository;
    this.vectorDBRepository = vectorDBRepository;
    this.embeddingService = embeddingService;
  }

  @Override
  public void run(ApplicationArguments args) {
    final var books = rakutenApiClient.streamBooks();

    try {
      dynamoDBRepository.refresh();
      vectorDBRepository.clearAll();
    } catch (DynamoDBRepositoryException e) {
      logger.error("Failed to refresh clothing", e);
      throw new RuntimeException(e);
    } catch (VectorDbClientException e) {
      logger.error("Failed to clear vector DB", e);
      throw new RuntimeException(e);
    }

    books.forEach(
        book -> {
          try {
            dynamoDBRepository.save(BookDto.from(book));
            vectorDBRepository.enqueueUpsert(book, createEmbedding(book.itemCaption()));
          } catch (EmbeddingServiceException | VectorDbClientException e) {
            logger.warn("Failed to upsert to vector DB for book with id: {}", book.id(), e);
          }
        });

    // キューの残りを送信
    try {
      vectorDBRepository.sendPendingUpserts();
    } catch (VectorDbClientException e) {
      logger.error("Failed to send pending upserts", e);
      throw new RuntimeException(e);
    }
  }

  private float[] createEmbedding(String caption) throws EmbeddingServiceException {
    if (caption == null || caption.isEmpty()) {
      return new float[0]; // キャプションが空の場合は空のベクトルを返す
    }
    return embeddingService.embedPassage(caption);
  }
}

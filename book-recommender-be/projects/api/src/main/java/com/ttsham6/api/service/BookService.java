package com.ttsham6.api.service;

import com.ttsham6.shared.domain.Book;
import com.ttsham6.shared.infra.DynamoDBRepository;
import com.ttsham6.shared.infra.VectorDBRepository;
import com.ttsham6.shared.infra.VectorDbClientException;
import com.ttsham6.shared.infra.dto.BookDto;
import com.ttsham6.shared.service.EmbeddingServiceException;
import com.ttsham6.shared.service.LocalEmbeddingService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BookService {
  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(BookService.class);
  private static final int LIMIT = 10;

  private final DynamoDBRepository dynamoDBRepository;
  private final LocalEmbeddingService embeddingService;
  private final VectorDBRepository vectorDBRepository;
  private final float similarityThreshold;

  public BookService(
      DynamoDBRepository dynamoDBRepository,
      LocalEmbeddingService embeddingService,
      VectorDBRepository vectorDBRepository,
      @Value("${book-recommender.search.similarity-threshold:0.7}") float similarityThreshold) {
    this.dynamoDBRepository = dynamoDBRepository;
    this.embeddingService = embeddingService;
    this.vectorDBRepository = vectorDBRepository;
    this.similarityThreshold = similarityThreshold;
  }

  public List<Book> getBooksByTitle(String query) {
    return dynamoDBRepository.getBooksByTitle(query).stream()
        .map(BookDto::toBook)
        .limit(LIMIT)
        .toList();
  }

  public List<Book> similarSearch(String query) {
    try {
      // embedding
      final var queryEmbedding = embeddingService.embedQuery(query);

      // Convert float[] to List<Float>
      final var queryEmbeddingList = new ArrayList<Float>();
      for (final var value : queryEmbedding) {
        queryEmbeddingList.add(value);
      }

      // ベクトル検索を実行
      final var vectorResultStream =
          vectorDBRepository.streamSearchSimilarBook(queryEmbeddingList, LIMIT * 2, null);

      // DynamoDBから実際のアイテムを取得
      return vectorResultStream
          .filter(item -> item.score() >= similarityThreshold)
          .map(item -> dynamoDBRepository.findById(item.id()))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map(BookDto::toBook)
          .limit(LIMIT)
          .toList();
    } catch (VectorDbClientException e) {
      logger.error("Vector search failed", e);
      return new ArrayList<>();
    } catch (EmbeddingServiceException e) {
      throw new RuntimeException(e);
    }
  }
}

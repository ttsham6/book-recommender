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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SimilarSearchService {
  private static final Logger logger = LoggerFactory.getLogger(SimilarSearchService.class);
  private static final int LIMIT = 10;

  private final LocalEmbeddingService embeddingService;
  private final VectorDBRepository vectorDBRepository;
  private final DynamoDBRepository dynamoDBRepository;

  public SimilarSearchService(
      LocalEmbeddingService embeddingService,
      VectorDBRepository vectorDBRepository,
      DynamoDBRepository dynamoDBRepository) {
    this.embeddingService = embeddingService;
    this.vectorDBRepository = vectorDBRepository;
    this.dynamoDBRepository = dynamoDBRepository;
  }

  public List<Book> search(String query) {
    try {
      // embedding
      final var queryEmbedding = embeddingService.embedQuery(query);

      // Convert float[] to List<Float>
      final var embeddingList = new ArrayList<Float>();
      for (final var value : queryEmbedding) {
        embeddingList.add(value);
      }

      // ベクトル検索を実行
      final var vectorResultStream =
          vectorDBRepository.streamSearchSimilarClothing(embeddingList, LIMIT * 2, null);

      // DynamoDBから実際のアイテムを取得
      return vectorResultStream
          .map(item -> dynamoDBRepository.findById(item.id()))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map(BookDto::toBook)
          .toList();
    } catch (VectorDbClientException e) {
      logger.error("Vector search failed", e);
      return new ArrayList<>();
    } catch (EmbeddingServiceException e) {
      throw new RuntimeException(e);
    }
  }
}

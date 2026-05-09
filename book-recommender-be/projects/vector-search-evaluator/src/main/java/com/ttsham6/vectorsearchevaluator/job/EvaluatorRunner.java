package com.ttsham6.vectorsearchevaluator.job;

import com.ttsham6.shared.infra.DynamoDBRepository;
import com.ttsham6.shared.infra.VectorDBRepository;
import com.ttsham6.shared.service.LocalEmbeddingService;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class EvaluatorRunner implements ApplicationRunner {
  private static final Logger logger = LoggerFactory.getLogger(EvaluatorRunner.class);
  private static final int LIMIT = 10;

  private final LocalEmbeddingService embeddingService;
  private final VectorDBRepository vectorDBRepository;
  private final DynamoDBRepository dynamoDBRepository;

  public EvaluatorRunner(
      LocalEmbeddingService embeddingService,
      VectorDBRepository vectorDBRepository,
      DynamoDBRepository dynamoDBRepository) {
    this.embeddingService = embeddingService;
    this.vectorDBRepository = vectorDBRepository;
    this.dynamoDBRepository = dynamoDBRepository;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    final var queries = args.getOptionValues("query");
    if (queries == null || queries.size() != 1) {
      throw new IllegalArgumentException(
          "Query is required. Please provide a query using --query option.");
    }
    final var query = queries.getFirst();

    final var queryEmbedding = embeddingService.embedQuery(query);
    final var queryEmbeddingList = new ArrayList<Float>();
    for (final var value : queryEmbedding) {
      queryEmbeddingList.add(value);
    }

    final var vectorResults =
        vectorDBRepository.streamSearchSimilarBook(queryEmbeddingList, LIMIT * 2, null);

    final var results =
        vectorResults.map(
            vectorResult -> {
              final var dbResult = dynamoDBRepository.findById(vectorResult.id()).orElseThrow();
              return new SearchResult(
                  vectorResult.id(), dbResult.itemCaption(), vectorResult.score());
            });

    // 結果をログに表示
    logger.info("Query: {}", query);
    results.forEach(
        result -> {
          logger.info(" ");
          logger.info("{}: {}", result.id(), result.score());
          logger.info("{}", result.caption());
        });
  }

  private record SearchResult(String id, String caption, float score) {}
}

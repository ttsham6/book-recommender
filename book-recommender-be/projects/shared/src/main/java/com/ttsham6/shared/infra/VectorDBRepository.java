package com.ttsham6.shared.infra;

import static io.pinecone.shadow.io.grpc.Status.Code.NOT_FOUND;

import com.ttsham6.shared.domain.Book;
import com.ttsham6.shared.domain.SimilaritySearchResult;
import io.pinecone.clients.Pinecone;
import io.pinecone.exceptions.PineconeValidationException;
import io.pinecone.shadow.io.grpc.StatusRuntimeException;
import io.pinecone.unsigned_indices_model.VectorWithUnsignedIndices;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VectorDBRepository {
  private static final Logger logger = LoggerFactory.getLogger(VectorDBRepository.class);
  private static final String NAMESPACE = "book";
  private static final int MAX_BATCH_SIZE = 100;

  private final Pinecone pineconeClient;
  private final String pineconeIndex;
  private final List<VectorWithUnsignedIndices> pendingVectors = new ArrayList<>();

  public VectorDBRepository(
      Pinecone pineconeClient, @Value("${book-recommender.pinecone.index}") String pineconeIndex) {
    this.pineconeClient = pineconeClient;
    this.pineconeIndex = pineconeIndex;
  }

  /** MAX_BATCH_SIZE まで貯めてまとめて送信する */
  public synchronized void enqueueUpsert(Book book, float[] embedding)
      throws VectorDbClientException {
    final var vector = createVector(book, embedding);
    if (vector == null) {
      logger.debug("Skip vector upsert because caption is empty. bookId={}", book.id());
      return;
    }

    pendingVectors.add(vector);
    if (pendingVectors.size() >= MAX_BATCH_SIZE) {
      sendPendingUpserts();
    }
  }

  /** VectorDB に送信しキューをクリアする */
  public synchronized void sendPendingUpserts() throws VectorDbClientException {
    if (pendingVectors.isEmpty()) {
      return;
    }

    final var vectorsToFlush = new ArrayList<>(pendingVectors);
    sendVectors(vectorsToFlush);
    pendingVectors.clear(); // クリア
  }

  private VectorWithUnsignedIndices createVector(Book book, float[] embedding) {
    if (book.itemCaption() == null || book.itemCaption().isEmpty()) {
      return null;
    }

    final var floatList = new ArrayList<Float>();
    for (float v : embedding) {
      floatList.add(v);
    }
    return new VectorWithUnsignedIndices(book.id(), floatList);
  }

  private void sendVectors(List<VectorWithUnsignedIndices> vectors) throws VectorDbClientException {
    if (vectors.isEmpty()) {
      logger.info("No vectors to upsert. index={}", pineconeIndex);
      return;
    }

    try {
      final var index = pineconeClient.getIndexConnection(pineconeIndex);
      final var res = index.upsert(vectors, NAMESPACE);

      if (res.getUpsertedCount() != vectors.size()) {
        logger.warn(
            "Not all vectors were upserted. expected={}, actual={}",
            vectors.size(),
            res.getUpsertedCount());
      } else {
        logger.info(
            "Successfully upserted vectors. count={}, index={}", vectors.size(), pineconeIndex);
      }
    } catch (PineconeValidationException e) {
      throw new VectorDbClientException("Failed to upsert vectors", e);
    }
  }

  public Stream<SimilaritySearchResult> streamSearchSimilarClothing(
      List<Float> embeddingList, int limit, Map<String, String> filterMetadata)
      throws VectorDbClientException {
    try {
      // Build the query request
      final var index = pineconeClient.getIndexConnection(pineconeIndex);
      // Query with optional filters
      final var results = index.queryByVector(limit, embeddingList, NAMESPACE);
      // Convert results to SimilaritySearchResult list
      return results.getMatchesList().stream()
          .map(match -> new SimilaritySearchResult(match.getId(), match.getScore()));
    } catch (Exception e) {
      throw new VectorDbClientException("Failed to search similar book", e);
    }
  }

  public void clearAll() throws VectorDbClientException {
    try {
      final var index = pineconeClient.getIndexConnection(pineconeIndex);
      index.deleteAll(NAMESPACE);
      logger.info(
          "Cleared all vectors in Pinecone index={}, namespace={}", pineconeIndex, NAMESPACE);
    } catch (StatusRuntimeException e) {
      if (NOT_FOUND.equals(e.getStatus().getCode())) {
        logger.warn(
            "Pinecone index or namespace not found when trying to clear vectors. Index; {}, Namespace: {}.",
            pineconeIndex,
            NAMESPACE);
        return;
      }
      throw new VectorDbClientException("Failed to clear vectors in Pinecone index", e);
    }
  }
}

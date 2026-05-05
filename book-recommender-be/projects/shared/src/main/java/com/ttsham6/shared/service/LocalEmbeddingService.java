package com.ttsham6.shared.service;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.ttsham6.shared.config.ModelProperty;
import com.ttsham6.shared.infra.ModelS3Client;
import com.ttsham6.shared.infra.ModelS3ClientException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LocalEmbeddingService implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(LocalEmbeddingService.class);
  private static final String QUANTIZED_MODEL_FILE_NAME = "model_quantized.onnx";
  private static final String TOKENIZER_FILE_NAME = "tokenizer.json";

  private final ModelProperty modelProperty;
  private final ModelS3Client modelS3Client;
  private OrtEnvironment env;
  private OrtSession session;
  private HuggingFaceTokenizer tokenizer;

  public LocalEmbeddingService(ModelProperty modelProperty, ModelS3Client modelS3Client) {
    this.modelProperty = modelProperty;
    this.modelS3Client = modelS3Client;
  }

  public void init() throws EmbeddingServiceException {
    try {
      modelS3Client.downloadModel(
          modelProperty.s3().bucket(), modelProperty.s3().prefix(), modelProperty.dir());
    } catch (ModelS3ClientException e) {
      throw new EmbeddingServiceException(
          "Failed to initialize LocalEmbeddingService due to model download failure", e);
    }

    try {
      final var modelDir = Paths.get(modelProperty.dir());
      final var onnxPath = requireFile(modelDir, QUANTIZED_MODEL_FILE_NAME);
      final var tokenizerPath = requireFile(modelDir, TOKENIZER_FILE_NAME);

      this.env = OrtEnvironment.getEnvironment();
      this.session = env.createSession(onnxPath.toString(), new OrtSession.SessionOptions());
      this.tokenizer = HuggingFaceTokenizer.newInstance(tokenizerPath);

      logger.info("LocalEmbeddingService initialized successfully with quantized ONNX model.");
    } catch (Exception e) {
      throw new EmbeddingServiceException("Failed to initialize LocalEmbeddingService", e);
    }
  }

  private Path requireFile(Path modelDir, String fileName) {
    final var path = modelDir.resolve(fileName);
    if (!Files.exists(path)) {
      throw new IllegalStateException("Required model file not found: " + path);
    }
    return path;
  }

  public float[] embedPassage(String text) throws EmbeddingServiceException {
    return embedInternal("passage: " + text);
  }

  public float[] embedQuery(String text) throws EmbeddingServiceException {
    return embedInternal("query: " + text);
  }

  private float[] embedInternal(String text) throws EmbeddingServiceException {
    if (session == null || tokenizer == null) {
      throw new IllegalStateException(
          "Embedding service is not initialized. Ensure init() was called successfully.");
    }

    try {
      // Batch size = 1
      final var encoding = tokenizer.encode(text);
      final long[][] inputIdsBatch = {encoding.getIds()};
      final long[][] attentionMaskBatch = {encoding.getAttentionMask()};
      final long[][] tokenTypeIdsBatch = {encoding.getTypeIds()};

      try (var inputIdsTensor = OnnxTensor.createTensor(env, inputIdsBatch);
          final var attentionMaskTensor = OnnxTensor.createTensor(env, attentionMaskBatch);
          final var tokenTypeIdsTensor = OnnxTensor.createTensor(env, tokenTypeIdsBatch)) {

        final var inputs =
            Map.<String, OnnxTensor>of(
                "input_ids", inputIdsTensor,
                "attention_mask", attentionMaskTensor,
                "token_type_ids", tokenTypeIdsTensor);

        try (var ortSessionResults = session.run(inputs)) {
          final var lastHiddenStateTensor = (OnnxTensor) ortSessionResults.get(0);
          final float[][][] lastHiddenState = (float[][][]) lastHiddenStateTensor.getValue();
          final float[] pooled = meanPooling(lastHiddenState, attentionMaskBatch);
          return l2Normalize(pooled);
        }
      }
    } catch (Exception e) {
      throw new EmbeddingServiceException("Embedding generation failed", e);
    }
  }

  private float[] meanPooling(float[][][] lastHiddenState, long[][] attentionMask) {
    final int seqLen = lastHiddenState[0].length;
    final int hiddenSize = lastHiddenState[0][0].length;
    final float[] sum = new float[hiddenSize];

    int count = 0;
    for (int i = 0; i < seqLen; i++) {
      if (attentionMask[0][i] == 1) {
        for (int j = 0; j < hiddenSize; j++) {
          sum[j] += lastHiddenState[0][i][j];
        }
        count++;
      }
    }

    if (count > 0) {
      for (int j = 0; j < hiddenSize; j++) {
        sum[j] /= count;
      }
    }
    return sum;
  }

  private float[] l2Normalize(float[] vector) {
    float sumSq = 0;
    for (float v : vector) {
      sumSq += v * v;
    }
    final float norm = (float) Math.sqrt(sumSq);
    if (norm > 1e-9) {
      for (int i = 0; i < vector.length; i++) {
        vector[i] /= norm;
      }
    }
    return vector;
  }

  @Override
  public void close() {
    try {
      if (session != null) {
        session.close();
      }
      if (env != null) {
        env.close();
      }
      if (tokenizer != null) {
        tokenizer.close();
      }
    } catch (Exception e) {
      logger.error("Error closing LocalEmbeddingService resources", e);
    }
  }
}

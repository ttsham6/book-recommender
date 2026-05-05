package com.ttsham6.shared.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.extensions.OrtxPackage;
import com.ttsham6.shared.config.ModelProperty;
import com.ttsham6.shared.infra.ModelS3Client;
import com.ttsham6.shared.infra.ModelS3ClientException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LocalEmbeddingService implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(LocalEmbeddingService.class);
  private static final String QUANTIZED_MODEL_FILE_NAME = "model_quantized.onnx";
  private static final String TOKENIZER_MODEL_FILE_NAME = "tokenizer.onnx";

  private final ModelProperty modelProperty;
  private final ModelS3Client modelS3Client;
  private OrtEnvironment env;
  private OrtSession modelSession;
  private OrtSession tokenizerSession;
  private String tokenizerInputName;
  private boolean modelRequiresTokenTypeIds;

  public LocalEmbeddingService(ModelProperty modelProperty, ModelS3Client modelS3Client) {
    this.modelProperty = modelProperty;
    this.modelS3Client = modelS3Client;
  }

  /** 起動時にS3からモデル一式を取得し、tokenizer用ONNXと推論用ONNXのセッションを初期化する。 */
  public void init() throws EmbeddingServiceException {
    try {
      modelS3Client.downloadModel(
          modelProperty.s3().bucket(), modelProperty.s3().prefix(), modelProperty.dir());
    } catch (ModelS3ClientException e) {
      throw new EmbeddingServiceException(
          "Failed to initialize LocalEmbeddingService due to model download failure", e);
    }

    try {
      initializeSessions(Paths.get(modelProperty.dir()));

      logger.info("LocalEmbeddingService initialized successfully with quantized ONNX model.");
    } catch (Exception e) {
      throw new EmbeddingServiceException("Failed to initialize LocalEmbeddingService", e);
    }
  }

  /** モデル配置ディレクトリから必要ファイルを見つけ、tokenizer用と推論用のONNXセッションを構築する。 */
  private void initializeSessions(Path modelDir) throws Exception {
    this.env = OrtEnvironment.getEnvironment();
    final var modelPath = requireFile(modelDir, QUANTIZED_MODEL_FILE_NAME);
    final var tokenizerModelPath = requireFile(modelDir, TOKENIZER_MODEL_FILE_NAME);
    this.tokenizerSession = createTokenizerSession(tokenizerModelPath);
    this.modelSession = createModelSession(modelPath);
    this.tokenizerInputName = tokenizerSession.getInputNames().iterator().next();
    this.modelRequiresTokenTypeIds = modelSession.getInputNames().contains("token_type_ids");
    logger.info("tokenizer output names={}", tokenizerSession.getOutputNames());
    logger.info("model input names={}", modelSession.getInputNames());
  }

  /** 埋め込み推論本体のONNXセッションを作成する。 */
  private OrtSession createModelSession(Path modelPath) throws Exception {
    return env.createSession(modelPath.toString());
  }

  /**
   * tokenizer用ONNXセッションを作成する。
   *
   * <p>tokenizer.onnx は ONNX Runtime Extensions の custom op に依存するため、 セッション作成前に拡張ライブラリを登録する。
   */
  private OrtSession createTokenizerSession(Path tokenizerModelPath) throws Exception {
    final var sessionOptions = new OrtSession.SessionOptions();
    try {
      sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath());
      return env.createSession(tokenizerModelPath.toString(), sessionOptions);
    } finally {
      sessionOptions.close();
    }
  }

  /** 指定ディレクトリ配下に対象ファイルが存在することを確認し、Pathを返す。 */
  private Path requireFile(Path modelDir, String fileName) {
    final var path = modelDir.resolve(fileName);
    if (!Files.exists(path)) {
      throw new IllegalStateException("Required model file not found: " + path);
    }
    return path;
  }

  /** 書籍本文向けの接頭辞を付けて埋め込みベクトルを生成する。 */
  public float[] embedPassage(String text) throws EmbeddingServiceException {
    return embedInternal("passage: " + text);
  }

  /** 検索クエリ向けの接頭辞を付けて埋め込みベクトルを生成する。 */
  public float[] embedQuery(String text) throws EmbeddingServiceException {
    return embedInternal("query: " + text);
  }

  /**
   * 文字列をtokenizer.onnxでテンソル化し、model_quantized.onnxで埋め込みを推論する。
   *
   * <p>モデル出力はトークン単位の hidden state なので、平均プーリングと L2 正規化をJava側で行い、最終的な埋め込みベクトルに変換する。
   *
   * <p>処理の流れは次の通り。
   *
   * <p>1. 入力文字列を tokenizer.onnx に渡して input_ids / attention_mask / token_type_ids を得る。
   *
   * <p>2. 得られたトークン列を model_quantized.onnx に渡して、各トークンの hidden state を得る。
   *
   * <p>3. attention_mask を使って有効トークンだけを平均し、1本のベクトルにまとめる。
   *
   * <p>4. ベクトルを L2 正規化し、類似度計算しやすい埋め込みへ整える。
   */
  private float[] embedInternal(String text) throws EmbeddingServiceException {
    if (modelSession == null || tokenizerSession == null || tokenizerInputName == null) {
      throw new IllegalStateException(
          "Embedding service is not initialized. Ensure init() was called successfully.");
    }

    try {
      final var modelInputs = tokenize(text);
      try (var inputIdsTensor = createLongTensor(modelInputs.inputIds());
          var attentionMaskTensor = createLongTensor(modelInputs.attentionMask());
          var tokenTypeIdsTensor = createLongTensor(modelInputs.tokenTypeIds())) {
        final var inputs = new LinkedHashMap<String, OnnxTensor>();
        inputs.put("input_ids", inputIdsTensor);
        inputs.put("attention_mask", attentionMaskTensor);
        if (modelRequiresTokenTypeIds) {
          inputs.put("token_type_ids", tokenTypeIdsTensor);
        }

        try (var ortSessionResults = modelSession.run(inputs)) {
          final var lastHiddenStateTensor = (OnnxTensor) ortSessionResults.get(0);
          final float[][][] lastHiddenState = (float[][][]) lastHiddenStateTensor.getValue();
          final float[] pooled =
              meanPooling(lastHiddenState, new long[][] {modelInputs.attentionMask()});
          return l2Normalize(pooled);
        }
      }
    } catch (Exception e) {
      throw new EmbeddingServiceException("Embedding generation failed", e);
    }
  }

  /**
   * tokenizer.onnx を実行し、model_quantized.onnx に渡す input_ids / attention_mask / token_type_ids を取り出す。
   *
   * <p>token_type_ids を使わないモデルもあるため、その場合は 0 埋め配列を補完する。 attention_mask が出力されない場合は、全トークン有効として 1
   * 埋め配列を補完する。
   */
  private ModelInputs tokenize(String text) throws Exception {
    try (var textTensor = OnnxTensor.createTensor(env, new String[] {text}, new long[] {1});
        var ortSessionResults = tokenizerSession.run(Map.of(tokenizerInputName, textTensor))) {
      long[] inputIds = null;
      long[] attentionMask = null;
      long[] tokenTypeIds = null;

      for (final var outputName : tokenizerSession.getOutputNames()) {
        final var outputValues = getTokenizerOutput(ortSessionResults, outputName);
        if (outputValues == null) continue;
        final var normalizedName = outputName.toLowerCase();
        logger.info("tokenizer output name={}, length={}", outputName, outputValues.length);

        if ((normalizedName.contains("input") && normalizedName.contains("id"))
            || normalizedName.equals("tokens")) {
          inputIds = outputValues;
        } else if (normalizedName.contains("mask")) {
          attentionMask = outputValues;
        } else if (normalizedName.contains("type")) {
          tokenTypeIds = outputValues;
        }
      }

      if (inputIds == null) {
        throw new IllegalStateException("Failed to resolve input ids from tokenizer.onnx outputs.");
      }
      attentionMask =
          attentionMask != null ? attentionMask : createFilledArray(inputIds.length, 1L);
      tokenTypeIds = tokenTypeIds != null ? tokenTypeIds : new long[inputIds.length];
      logger.info(
          "resolved tokenizer outputs input_ids={}, attention_mask={}, token_type_ids={}",
          inputIds.length,
          attentionMask.length,
          tokenTypeIds.length);
      return new ModelInputs(inputIds, attentionMask, tokenTypeIds);
    }
  }

  /** 1次元の long 配列を、モデル入力用の shape [1, sequence_length] テンソルへ変換する。 */
  private OnnxTensor createLongTensor(long[] values) throws Exception {
    return OnnxTensor.createTensor(env, new long[][] {values});
  }

  /** tokenizer セッション結果から指定出力を取得し、long 配列へ正規化して返す。 */
  private long[] getTokenizerOutput(OrtSession.Result result, String outputName) throws Exception {
    final var value = result.get(outputName);
    return value.isPresent() ? extractLongArray((OnnxTensor) value.get()) : null;
  }

  /** 指定長の long 配列を同一値で埋めて返す。 */
  private long[] createFilledArray(int length, long value) {
    final var values = new long[length];
    Arrays.fill(values, value);
    return values;
  }

  /**
   * tokenizer ONNX 出力を long 配列へ変換する。
   *
   * <p>生成された tokenizer.onnx によって int32 / int64 のどちらもあり得るため、 両方を受けられるようにする。
   */
  private long[] extractLongArray(OnnxTensor tensor) throws Exception {
    final var value = tensor.getValue();
    if (value instanceof long[] longArray) {
      return longArray;
    }
    if (value instanceof long[][] longMatrix && longMatrix.length > 0) {
      return longMatrix[0];
    }
    if (value instanceof int[] intArray) {
      return toLongArray(intArray);
    }
    if (value instanceof int[][] intMatrix && intMatrix.length > 0) {
      return toLongArray(intMatrix[0]);
    }
    throw new IllegalStateException("Unexpected preprocess output type: " + value.getClass());
  }

  /** int 配列を long 配列へ変換する。 */
  private long[] toLongArray(int[] values) {
    final var converted = new long[values.length];
    for (int i = 0; i < values.length; i++) {
      converted[i] = values[i];
    }
    return converted;
  }

  /** attention mask が 1 のトークンだけを対象に hidden state を平均し、1本のベクトルへ縮約する。 */
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

  /**
   * ベクトル長が 1 になるよう L2 正規化する。
   *
   * <p>コサイン類似度ベースの検索で、ベクトルの大きさではなく向きだけを比較できるようにする。
   */
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

  /** ONNX Runtime セッションと環境を解放する。 */
  @Override
  public void close() {
    try {
      if (modelSession != null) {
        modelSession.close();
      }
      if (tokenizerSession != null) {
        tokenizerSession.close();
      }
      if (env != null) {
        env.close();
      }
    } catch (Exception e) {
      logger.error("Error closing LocalEmbeddingService resources", e);
    }
  }

  private record ModelInputs(long[] inputIds, long[] attentionMask, long[] tokenTypeIds) {}
}

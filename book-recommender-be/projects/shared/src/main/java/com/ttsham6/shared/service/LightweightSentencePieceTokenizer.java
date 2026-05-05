package com.ttsham6.shared.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class LightweightSentencePieceTokenizer {
  private static final String METASPACE = "\u2581";
  private static final int DEFAULT_MODEL_MAX_LENGTH = 512;
  private static final float UNKNOWN_PENALTY = -1_000_000f;

  private final TrieNode trieRoot;
  private final int bosTokenId;
  private final int eosTokenId;
  private final int unkTokenId;
  private final int modelMaxLength;

  private LightweightSentencePieceTokenizer(
      TrieNode trieRoot, int bosTokenId, int eosTokenId, int unkTokenId, int modelMaxLength) {
    this.trieRoot = trieRoot;
    this.bosTokenId = bosTokenId;
    this.eosTokenId = eosTokenId;
    this.unkTokenId = unkTokenId;
    this.modelMaxLength = modelMaxLength;
  }

  static LightweightSentencePieceTokenizer fromTokenizerJson(Path tokenizerJsonPath)
      throws IOException {
    final var trieRoot = new TrieNode();
    final var specialTokenIds = new HashMap<String, Integer>();
    int unkTokenId = 3;
    int modelMaxLength = DEFAULT_MODEL_MAX_LENGTH;

    final var jsonFactory = new JsonFactory();
    try (var parser = jsonFactory.createParser(tokenizerJsonPath.toFile())) {
      while (parser.nextToken() != null) {
        if (parser.currentToken() != JsonToken.FIELD_NAME) {
          continue;
        }

        final var fieldName = parser.currentName();
        switch (fieldName) {
          case "added_tokens" -> readAddedTokens(parser, specialTokenIds);
          case "model" -> {
            final var modelMetadata = readModel(parser, trieRoot);
            unkTokenId = modelMetadata.unkTokenId();
          }
          case "truncation" -> modelMaxLength = readModelMaxLength(parser, modelMaxLength);
          default -> {
            parser.nextToken();
            parser.skipChildren();
          }
        }
      }
    }

    return new LightweightSentencePieceTokenizer(
        trieRoot,
        specialTokenIds.getOrDefault("<s>", 0),
        specialTokenIds.getOrDefault("</s>", 2),
        specialTokenIds.getOrDefault("<unk>", unkTokenId),
        modelMaxLength);
  }

  private static void readAddedTokens(JsonParser parser, Map<String, Integer> specialTokenIds)
      throws IOException {
    if (parser.nextToken() != JsonToken.START_ARRAY) {
      parser.skipChildren();
      return;
    }

    while (parser.nextToken() != JsonToken.END_ARRAY) {
      String content = null;
      Integer id = null;
      while (parser.nextToken() != JsonToken.END_OBJECT) {
        final var fieldName = parser.currentName();
        parser.nextToken();
        if ("content".equals(fieldName)) {
          content = parser.getValueAsString();
        } else if ("id".equals(fieldName)) {
          id = parser.getIntValue();
        } else {
          parser.skipChildren();
        }
      }
      if (content != null && id != null) {
        specialTokenIds.put(content, id);
      }
    }
  }

  private static ModelMetadata readModel(JsonParser parser, TrieNode trieRoot) throws IOException {
    int unkTokenId = 3;
    if (parser.nextToken() != JsonToken.START_OBJECT) {
      parser.skipChildren();
      return new ModelMetadata(unkTokenId);
    }

    int tokenId = 0;
    while (parser.nextToken() != JsonToken.END_OBJECT) {
      final var fieldName = parser.getCurrentName();
      parser.nextToken();
      if ("unk_id".equals(fieldName)) {
        unkTokenId = parser.getIntValue();
      } else if ("vocab".equals(fieldName) && parser.currentToken() == JsonToken.START_ARRAY) {
        while (parser.nextToken() != JsonToken.END_ARRAY) {
          if (parser.currentToken() != JsonToken.START_ARRAY) {
            parser.skipChildren();
            continue;
          }

          parser.nextToken();
          final var piece = parser.getValueAsString();
          parser.nextToken();
          final var score = (float) parser.getDoubleValue();
          while (parser.nextToken() != JsonToken.END_ARRAY) {
            parser.skipChildren();
          }

          if (!piece.startsWith("<") || !piece.endsWith(">")) {
            insertPiece(trieRoot, piece, tokenId, score);
          }
          tokenId++;
        }
      } else {
        parser.skipChildren();
      }
    }

    return new ModelMetadata(unkTokenId);
  }

  private static int readModelMaxLength(JsonParser parser, int defaultValue) throws IOException {
    if (parser.nextToken() != JsonToken.START_OBJECT) {
      parser.skipChildren();
      return defaultValue;
    }

    int maxLength = defaultValue;
    while (parser.nextToken() != JsonToken.END_OBJECT) {
      final var fieldName = parser.getCurrentName();
      parser.nextToken();
      if ("max_length".equals(fieldName) && parser.currentToken().isNumeric()) {
        maxLength = parser.getIntValue();
      } else {
        parser.skipChildren();
      }
    }
    return maxLength;
  }

  private static void insertPiece(TrieNode root, String piece, int tokenId, float score) {
    var current = root;
    for (int i = 0; i < piece.length(); i++) {
      current = current.children.computeIfAbsent(piece.charAt(i), unused -> new TrieNode());
    }
    current.tokenId = tokenId;
    current.score = score;
  }

  private static String normalize(String text) {
    final var collapsedSpaces =
        Normalizer.normalize(text, Normalizer.Form.NFKC).replaceAll(" {2,}", " ").strip();
    if (collapsedSpaces.isEmpty()) {
      return METASPACE;
    }

    final var metaspaceText = collapsedSpaces.replace(" ", METASPACE);
    if (metaspaceText.startsWith(METASPACE)) {
      return metaspaceText;
    }
    return METASPACE + metaspaceText;
  }

  TokenEncoding encode(String text) {
    final var normalized = normalize(text);
    final var tokenIds = tokenize(normalized);
    final int contentTokenLimit = Math.max(0, modelMaxLength - 2);
    final int contentSize = Math.min(tokenIds.size(), contentTokenLimit);

    final long[] inputIds = new long[contentSize + 2];
    final long[] attentionMask = new long[contentSize + 2];
    final long[] tokenTypeIds = new long[contentSize + 2];

    inputIds[0] = bosTokenId;
    attentionMask[0] = 1L;

    for (int i = 0; i < contentSize; i++) {
      inputIds[i + 1] = tokenIds.get(i);
      attentionMask[i + 1] = 1L;
    }

    inputIds[inputIds.length - 1] = eosTokenId;
    attentionMask[attentionMask.length - 1] = 1L;
    return new TokenEncoding(inputIds, attentionMask, tokenTypeIds);
  }

  private List<Integer> tokenize(String normalizedText) {
    final int length = normalizedText.length();
    final float[] bestScores = new float[length + 1];
    final int[] previousIndex = new int[length + 1];
    final int[] previousTokenId = new int[length + 1];

    for (int i = 1; i <= length; i++) {
      bestScores[i] = Float.NEGATIVE_INFINITY;
      previousIndex[i] = -1;
      previousTokenId[i] = -1;
    }

    bestScores[0] = 0f;
    for (int start = 0; start < length; start++) {
      if (bestScores[start] == Float.NEGATIVE_INFINITY) {
        continue;
      }

      boolean matched = false;
      var node = trieRoot;
      for (int end = start; end < length; end++) {
        node = node.children.get(normalizedText.charAt(end));
        if (node == null) {
          break;
        }

        if (node.tokenId >= 0) {
          matched = true;
          final int next = end + 1;
          final float candidateScore = bestScores[start] + node.score;
          if (candidateScore > bestScores[next]) {
            bestScores[next] = candidateScore;
            previousIndex[next] = start;
            previousTokenId[next] = node.tokenId;
          }
        }
      }

      if (!matched) {
        final int next = start + Character.charCount(normalizedText.codePointAt(start));
        final float candidateScore = bestScores[start] + UNKNOWN_PENALTY;
        if (candidateScore > bestScores[next]) {
          bestScores[next] = candidateScore;
          previousIndex[next] = start;
          previousTokenId[next] = unkTokenId;
        }
      }
    }

    final var reversedTokens = new ArrayList<Integer>();
    int current = length;
    while (current > 0) {
      final int tokenId = previousTokenId[current];
      final int prev = previousIndex[current];
      if (tokenId < 0 || prev < 0) {
        reversedTokens.clear();
        reversedTokens.add(unkTokenId);
        break;
      }
      reversedTokens.add(tokenId);
      current = prev;
    }

    final var tokens = new ArrayList<Integer>(reversedTokens.size());
    for (int i = reversedTokens.size() - 1; i >= 0; i--) {
      tokens.add(reversedTokens.get(i));
    }
    return tokens;
  }

  record TokenEncoding(long[] ids, long[] attentionMask, long[] typeIds) {}

  private record ModelMetadata(int unkTokenId) {}

  private static final class TrieNode {
    private final Map<Character, TrieNode> children = new HashMap<>();
    private int tokenId = -1;
    private float score;
  }
}

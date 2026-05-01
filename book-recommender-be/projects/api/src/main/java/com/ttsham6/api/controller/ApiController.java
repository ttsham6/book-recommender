package com.ttsham6.api.controller;

import com.ttsham6.api.dto.BookResponse;
import com.ttsham6.api.service.SimilarSearchService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiController {
  private final SimilarSearchService similarSearchService;

  public ApiController(SimilarSearchService similarSearchService) {
    this.similarSearchService = similarSearchService;
  }

  /** ヘルスチェックエンドポイント */
  @GetMapping("/health")
  public Map<String, String> health() {
    return Map.of(
        "status", "healthy",
        "message", "API is healthy");
  }

  /** レコメンデーションを取得 */
  @GetMapping("/recommendations")
  public BookResponse getRecommendations(@RequestParam String query) {
    final var items = similarSearchService.search(query);
    return new BookResponse(items.size(), items);
  }
}

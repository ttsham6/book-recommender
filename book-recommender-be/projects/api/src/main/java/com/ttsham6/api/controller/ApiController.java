package com.ttsham6.api.controller;

import com.ttsham6.api.dto.BookResponse;
import com.ttsham6.api.service.BookService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiController {
  private final BookService bookService;

  public ApiController(BookService bookService) {
    this.bookService = bookService;
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
    final var items = bookService.similarSearch(query);
    return new BookResponse(items.size(), items);
  }

  @GetMapping("/item")
  public BookResponse getItem(@RequestParam String query) {
    final var items = bookService.getBooksByTitle(query);
    return new BookResponse(items.size(), items);
  }
}

package com.ttsham6.shared.external.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ttsham6.shared.domain.book.Book;
import java.util.List;

public record RakutenResponseDto(@JsonProperty("Items") List<RakutenItemWrapper> items) {

  public record RakutenItemWrapper(@JsonProperty("Item") Book item) {}
}

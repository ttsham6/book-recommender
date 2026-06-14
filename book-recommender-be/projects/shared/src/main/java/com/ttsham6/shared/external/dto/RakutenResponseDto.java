package com.ttsham6.shared.external.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RakutenResponseDto(@JsonProperty("Items") List<RakutenItemWrapper> items) {

  public record RakutenItemWrapper(@JsonProperty("Item") RakutenBookDto item) {}
}

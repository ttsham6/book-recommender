package com.ttsham6.shared.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;

public record Book(
    String id,
    String title,
    String author,
    @Nullable String artistName,
    String publisherName,
    @Nullable String label,
    String isbn,
    @Nullable String jan,
    @Nullable String hardware,
    @Nullable String os,
    String itemCaption,
    String salesDate,
    long itemPrice,
    long listPrice,
    double discountRate,
    double discountPrice,
    String itemUrl,
    @Nullable String affiliateUrl,
    @Nullable String smallImageUrl,
    @Nullable String mediumImageUrl,
    @Nullable String largeImageUrl,
    @Nullable String chirayomiUrl,
    Availability availability,
    PostageFlag postageFlag,
    boolean isLimited,
    long reviewCount,
    double reviewAverage,
    List<Integer> genreIds) {

  public Book {
    if (id == null) {
      id = UUID.randomUUID().toString();
    }
  }
}

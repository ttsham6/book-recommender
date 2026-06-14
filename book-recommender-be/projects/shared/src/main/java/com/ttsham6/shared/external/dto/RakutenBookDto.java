package com.ttsham6.shared.external.dto;

import com.ttsham6.shared.domain.Availability;
import com.ttsham6.shared.domain.Book;
import com.ttsham6.shared.domain.PostageFlag;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.lang.Nullable;

public record RakutenBookDto(
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
    String booksGenreId) {

  public Book toBook() {
    final var genreIds =
        Arrays.stream(booksGenreId.split("/")).map(String::valueOf).collect(Collectors.toList());

    return new Book(
        null,
        title,
        author,
        artistName,
        publisherName,
        label,
        isbn,
        jan,
        hardware,
        os,
        itemCaption,
        salesDate,
        itemPrice,
        listPrice,
        discountRate,
        discountPrice,
        itemUrl,
        affiliateUrl,
        smallImageUrl,
        mediumImageUrl,
        largeImageUrl,
        chirayomiUrl,
        availability,
        postageFlag,
        isLimited,
        reviewCount,
        reviewAverage,
        genreIds);
  }
}

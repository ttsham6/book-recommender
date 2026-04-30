package com.ttsham6.shared.infrastructure.dto;

import com.ttsham6.shared.domain.book.Availability;
import com.ttsham6.shared.domain.book.Book;
import com.ttsham6.shared.domain.book.PostageFlag;
import java.util.List;
import java.util.UUID;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbImmutable(builder = BookDto.Builder.class)
public record BookDto(
    String id,
    String title,
    String author,
    String artistName,
    String publisherName,
    String label,
    String isbn,
    String jan,
    String hardware,
    String os,
    String itemCaption,
    String salesDate,
    long itemPrice,
    long listPrice,
    double discountRate,
    double discountPrice,
    String itemUrl,
    String affiliateUrl,
    String smallImageUrl,
    String mediumImageUrl,
    String largeImageUrl,
    String chirayomiUrl,
    Availability availability,
    PostageFlag postageFlag,
    boolean limited,
    long reviewCount,
    double reviewAverage,
    List<Integer> genreIds) {

  public static BookDto from(Book book) {
    return BookDto.builder()
        .id(book.id() == null ? UUID.randomUUID().toString() : book.id())
        .title(book.title())
        .author(book.author())
        .artistName(book.artistName())
        .publisherName(book.publisherName())
        .label(book.label())
        .isbn(book.isbn())
        .jan(book.jan())
        .hardware(book.hardware())
        .os(book.os())
        .itemCaption(book.itemCaption())
        .salesDate(book.salesDate())
        .itemPrice(book.itemPrice())
        .listPrice(book.listPrice())
        .discountRate(book.discountRate())
        .discountPrice(book.discountPrice())
        .itemUrl(book.itemUrl())
        .affiliateUrl(book.affiliateUrl())
        .smallImageUrl(book.smallImageUrl())
        .mediumImageUrl(book.mediumImageUrl())
        .largeImageUrl(book.largeImageUrl())
        .chirayomiUrl(book.chirayomiUrl())
        .availability(book.availability())
        .postageFlag(book.postageFlag())
        .limited(book.isLimited())
        .reviewCount(book.reviewCount())
        .reviewAverage(book.reviewAverage())
        .genreIds(book.genreIds())
        .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  @DynamoDbPartitionKey
  public String id() {
    return id;
  }

  public static class Builder {
    private String id;
    private String title;
    private String author;
    private String artistName;
    private String publisherName;
    private String label;
    private String isbn;
    private String jan;
    private String hardware;
    private String os;
    private String itemCaption;
    private String salesDate;
    private long itemPrice;
    private long listPrice;
    private double discountRate;
    private double discountPrice;
    private String itemUrl;
    private String affiliateUrl;
    private String smallImageUrl;
    private String mediumImageUrl;
    private String largeImageUrl;
    private String chirayomiUrl;
    private Availability availability;
    private PostageFlag postageFlag;
    private boolean limited;
    private long reviewCount;
    private double reviewAverage;
    private List<Integer> genreIds;

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder author(String author) {
      this.author = author;
      return this;
    }

    public Builder artistName(String artistName) {
      this.artistName = artistName;
      return this;
    }

    public Builder publisherName(String publisherName) {
      this.publisherName = publisherName;
      return this;
    }

    public Builder label(String label) {
      this.label = label;
      return this;
    }

    public Builder isbn(String isbn) {
      this.isbn = isbn;
      return this;
    }

    public Builder jan(String jan) {
      this.jan = jan;
      return this;
    }

    public Builder hardware(String hardware) {
      this.hardware = hardware;
      return this;
    }

    public Builder os(String os) {
      this.os = os;
      return this;
    }

    public Builder itemCaption(String itemCaption) {
      this.itemCaption = itemCaption;
      return this;
    }

    public Builder salesDate(String salesDate) {
      this.salesDate = salesDate;
      return this;
    }

    public Builder itemPrice(long itemPrice) {
      this.itemPrice = itemPrice;
      return this;
    }

    public Builder listPrice(long listPrice) {
      this.listPrice = listPrice;
      return this;
    }

    public Builder discountRate(double discountRate) {
      this.discountRate = discountRate;
      return this;
    }

    public Builder discountPrice(double discountPrice) {
      this.discountPrice = discountPrice;
      return this;
    }

    public Builder itemUrl(String itemUrl) {
      this.itemUrl = itemUrl;
      return this;
    }

    public Builder affiliateUrl(String affiliateUrl) {
      this.affiliateUrl = affiliateUrl;
      return this;
    }

    public Builder smallImageUrl(String smallImageUrl) {
      this.smallImageUrl = smallImageUrl;
      return this;
    }

    public Builder mediumImageUrl(String mediumImageUrl) {
      this.mediumImageUrl = mediumImageUrl;
      return this;
    }

    public Builder largeImageUrl(String largeImageUrl) {
      this.largeImageUrl = largeImageUrl;
      return this;
    }

    public Builder chirayomiUrl(String chirayomiUrl) {
      this.chirayomiUrl = chirayomiUrl;
      return this;
    }

    public Builder availability(Availability availability) {
      this.availability = availability;
      return this;
    }

    public Builder postageFlag(PostageFlag postageFlag) {
      this.postageFlag = postageFlag;
      return this;
    }

    public Builder limited(boolean limited) {
      this.limited = limited;
      return this;
    }

    public Builder reviewCount(long reviewCount) {
      this.reviewCount = reviewCount;
      return this;
    }

    public Builder reviewAverage(double reviewAverage) {
      this.reviewAverage = reviewAverage;
      return this;
    }

    public Builder genreIds(List<Integer> genreIds) {
      this.genreIds = genreIds;
      return this;
    }

    public BookDto build() {
      return new BookDto(
          id,
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
          limited,
          reviewCount,
          reviewAverage,
          genreIds);
    }
  }
}

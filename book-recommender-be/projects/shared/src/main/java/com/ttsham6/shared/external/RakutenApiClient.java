package com.ttsham6.shared.external;

import com.ttsham6.shared.config.RakutenApiProperty;
import com.ttsham6.shared.domain.book.Book;
import com.ttsham6.shared.external.dto.RakutenResponseDto;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class RakutenApiClient {

  private static final Logger logger = LoggerFactory.getLogger(RakutenApiClient.class);

  private static final int SIZE_ON_PAGE = 30;
  private static final String BOOK_GENRE_ID = "001021001";
  private static final String REFERER_URL = "https://book-recommender.com";

  private final RestTemplate rakutenRestTemplate;
  private final RakutenApiProperty property;

  public RakutenApiClient(RestTemplate rakutenRestTemplate, RakutenApiProperty property) {
    this.rakutenRestTemplate = rakutenRestTemplate;
    this.property = property;
  }

  public Stream<Book> streamBooks() {
    return IntStream.rangeClosed(1, property.getPageSize())
        .boxed()
        .flatMap(
            page -> {
              try {
                logger.info("Getting books for page {}", page);
                Thread.sleep(500);
                return streamBookByPage(page);
              } catch (RakutenApiException e) {
                logger.error("Failed to fetch books from Rakuten API for page {}", page, e);
                return Stream.empty();
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
            });
  }

  private Stream<Book> streamBookByPage(int page) throws RakutenApiException {
    try {
      final var url =
          UriComponentsBuilder.fromUri(new URI(property.getUrl()))
              .queryParam("applicationId", property.getApplicationId())
              .queryParam("accessKey", property.getAccessKey())
              .queryParam("format", "json")
              .queryParam("booksGenreId", BOOK_GENRE_ID)
              .queryParam("hits", SIZE_ON_PAGE)
              .queryParam("page", page)
              .toUriString();

      final var headers = new HttpHeaders();
      headers.set("Referer", REFERER_URL);
      headers.set("Origin", REFERER_URL);

      final var requestEntity = new HttpEntity<Void>(headers);
      final var responseEntity =
          rakutenRestTemplate.exchange(
              url, HttpMethod.GET, requestEntity, RakutenResponseDto.class);
      final var response = responseEntity.getBody();

      if (response == null || response.items() == null) {
        throw new RakutenApiException("Rakuten API response is null");
      }
      return response.items().stream().map(RakutenResponseDto.RakutenItemWrapper::item);
    } catch (URISyntaxException | RakutenApiException e) {
      throw new RakutenApiException("Failed to fetch books from Rakuten API", e);
    }
  }
}

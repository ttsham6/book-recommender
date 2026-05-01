package com.ttsham6.shared.job;

import com.ttsham6.shared.service.LocalEmbeddingService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class ModelDownloadRunner implements ApplicationRunner {
  private final LocalEmbeddingService embeddingService;

  public ModelDownloadRunner(LocalEmbeddingService embeddingService) {
    this.embeddingService = embeddingService;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    embeddingService.init();
  }
}

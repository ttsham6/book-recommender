package com.ttsham6.vectorsearchevaluator;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.ttsham6.vectorsearchevaluator", "com.ttsham6.shared"})
public class VectorSearchEvaluatorApplication {
  public static void main(String[] args) {
    new SpringApplicationBuilder(VectorSearchEvaluatorApplication.class)
        .web(WebApplicationType.NONE)
        .run(args);
  }
}
